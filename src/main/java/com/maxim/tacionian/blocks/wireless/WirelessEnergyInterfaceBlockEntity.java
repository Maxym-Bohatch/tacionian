package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessEnergyInterfaceBlockEntity extends BlockEntity {
    private int mode = 0; // 0:Safe (75%), 1:Balanced (40%), 2:Performance (15%), 3:Unrestricted (0%)

    // Додаємо власну капабіліті, щоб блок розпізнавався ТХ-кабелями
    private final ITachyonStorage tachyonHandler = new ITachyonStorage() {
        @Override public int receiveTacion(int amount, boolean simulate) { return 0; } // Інтерфейс тільки віддає енергію
        @Override public int extractTacion(int amount, boolean simulate) { return 0; }
        @Override public int getTacionStored() { return 0; }
        @Override public int getMaxTacionCapacity() { return 1000; }
    };

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> tachyonHandler);

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) {
            return tachyonHolder.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tachyonHolder.invalidate();
    }

    public void cycleMode(Player player) {
        this.mode = (this.mode + 1) % 4;
        if (level != null && !level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName()), true);
        }
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessEnergyInterfaceBlockEntity be) {
        if (level.isClientSide) return;

        AABB area = new AABB(pos).inflate(20);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        int threshold = switch (be.mode) {
            case 0 -> 75;
            case 1 -> 40;
            case 2 -> 15;
            default -> 0;
        };

        for (Player player : players) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                    pEnergy.setRemoteStabilized(true);

                    boolean machinesNeedPower = checkMachinesNeedPower(level, pos);
                    int currentPercent = pEnergy.getEnergyPercent();

                    if (machinesNeedPower) {
                        pEnergy.setRemoteNoDrain(false);

                        if (level.getGameTime() % 10 == 0) {
                            if (currentPercent > threshold) {
                                processEnergyTransfer(level, pos, serverPlayer, pEnergy);
                            }
                            pEnergy.sync(serverPlayer);
                        }
                    } else {
                        // Якщо база заповнена, але енергії забагато — скидаємо в повітря (івент для аддонів)
                        if (currentPercent > threshold) {
                            pEnergy.setRemoteNoDrain(true);

                            if (level.getGameTime() % 20 == 0) {
                                int waste = pEnergy.extractEnergyPure(25, false);
                                if (waste > 0) {
                                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                                            new com.maxim.tacionian.api.events.TachyonWasteEvent(level, pos, waste)
                                    );
                                }
                            }
                        } else {
                            pEnergy.setRemoteNoDrain(false);
                        }

                        if (level.getGameTime() % 10 == 0) {
                            pEnergy.sync(serverPlayer);
                        }
                    }
                });
            }
        }
    }

    private static boolean checkMachinesNeedPower(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                // Перевіряємо і RF, і TX системи
                boolean needsRF = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                        .map(cap -> cap.canReceive() && cap.receiveEnergy(10, true) > 0).orElse(false);
                boolean needsTX = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite())
                        .map(cap -> cap.getTacionStored() < cap.getMaxTacionCapacity()).orElse(false);

                if (needsRF || needsTX) return true;
            }
        }
        return false;
    }

    private static void processEnergyTransfer(Level level, BlockPos pos, ServerPlayer serverPlayer, com.maxim.tacionian.energy.PlayerEnergy pEnergy) {
        int txToTakeMax = 200;
        int txForRF = 0;
        int txForTX = 0;

        // 1. Рахуємо потреби
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            txForRF += neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                    .map(cap -> cap.receiveEnergy(txToTakeMax * 10, true) / 10).orElse(0);

            txForTX += neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite())
                    .map(cap -> cap.receiveTacion(txToTakeMax, true)).orElse(0);
        }

        if (txForRF <= 0 && txForTX <= 0) return;

        // 2. Розподіляємо та виконуємо передачу
        int totalExtracted = 0;
        int rfPart = 0;
        int txPart = 0;

        if (txForRF > 0 && txForTX > 0) {
            totalExtracted = Math.min(txToTakeMax, txForRF + txForTX);
            rfPart = totalExtracted / 2;
            txPart = totalExtracted / 2;
        } else if (txForRF > 0) {
            rfPart = Math.min(txToTakeMax, txForRF);
            totalExtracted = rfPart;
        } else {
            txPart = Math.min(txToTakeMax, txForTX);
            totalExtracted = txPart;
        }

        // Реально передаємо енергію в блоки
        if (rfPart > 0) distribute(level, pos, rfPart, true);
        if (txPart > 0) distribute(level, pos, txPart, false);

        // 3. РОЗДІЛЕННЯ ДОСВІДУ
        if (totalExtracted > 0) {
            // За ту частину, що пішла в RF — даємо досвід
            if (rfPart > 0) {
                pEnergy.extractEnergyWithExp(rfPart, false, serverPlayer);
            }

            // За ту частину, що пішла в ТХ (кабелі/сховища) — просто забираємо енергію
            if (txPart > 0) {
                pEnergy.extractEnergyPure(txPart, false);
            }
        }
    }


    private static void distribute(Level level, BlockPos pos, int amount, boolean isRF) {
        int remaining = isRF ? amount * 10 : amount;

        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;

            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                if (isRF) {
                    // Використовуємо звичайний ifPresent з оновленням зовнішньої змінної через масив або просту логіку
                    var cap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                    if (cap.isPresent()) {
                        int received = cap.orElse(null).receiveEnergy(remaining, false);
                        remaining -= received;
                    }
                } else {
                    var cap = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite());
                    if (cap.isPresent()) {
                        int received = cap.orElse(null).receiveTacion(remaining, false);
                        remaining -= received;
                    }
                }
            }
        }
    }


    public Component getModeName() {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe");
            case 1 -> Component.translatable("mode.tacionian.balanced");
            case 2 -> Component.translatable("mode.tacionian.performance");
            default -> Component.translatable("mode.tacionian.unrestricted");
        };
    }

    @Override protected void saveAdditional(CompoundTag nbt) {
        nbt.putInt("Mode", mode);
        super.saveAdditional(nbt);
    }

    @Override public void load(CompoundTag nbt) {
        super.load(nbt);
        this.mode = nbt.getInt("Mode");
    }
}