package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessEnergyInterfaceBlockEntity extends BlockEntity implements ITachyonStorage {
    private int mode = 0; // 0: Safe, 1: Balanced, 2: Performance, 3: Unrestricted
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 2000;

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> rfHolder = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            int toTakeTx = Math.min(storedEnergy, maxExtract / 10);
            if (!simulate && toTakeTx > 0) { storedEnergy -= toTakeTx; setChanged(); }
            return toTakeTx * 10;
        }
        @Override public int getEnergyStored() { return storedEnergy * 10; }
        @Override public int getMaxEnergyStored() { return MAX_CAPACITY * 10; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    });

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    // Доданий метод для перемикання режимів
    public void cycleMode(Player player) {
        this.mode = (this.mode + 1) % 4;
        setChanged();
        if (level != null && !level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName()), true);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessEnergyInterfaceBlockEntity be) {
        if (level.isClientSide) return;

        // 1. АВТОНОМНА ПЕРЕДАЧА ЕНЕРГІЇ (Працює завжди)
        // Шукаємо найближчого гравця в радіусі 20 блоків для нарахування досвіду
        ServerPlayer closestPlayer = (ServerPlayer) level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 20, true);
        be.processEnergyTransfer(level, pos, closestPlayer);

        // 2. ВЗАЄМОДІЯ З ГРАВЦЯМИ
        AABB area = new AABB(pos).inflate(20);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        // Поріг для збору енергії (Safe: 75%, Balanced: 40%, Performance: 15%, Unrestricted: 0%)
        int threshold = switch (be.mode) { case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 0; };

        boolean changed = false;
        for (Player player : players) {
            if (player instanceof ServerPlayer serverPlayer) {
                var cap = serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY);
                if (cap.isPresent()) {
                    var pEnergy = cap.orElse(null);

                    // ГОЛОВНЕ: Стабілізуємо гравця, щоб він не вибухнув поруч
                    pEnergy.setRemoteStabilized(true);

                    // Якщо енергія гравця вища за поріг режиму АБО гравець перевантажений (isOverloaded)
                    // Для 1 рівня перевантаження тепер 95%, але threshold 75% дозволить забирати раніше
                    if (pEnergy.getEnergyPercent() > threshold || pEnergy.isOverloaded()) {
                        int toExtract = 50; // Базова швидкість викачування
                        int extracted = pEnergy.extractEnergyPure(toExtract, false);
                        int accepted = be.receiveFromPlayer(extracted, false);

                        // Якщо буфер блока повний, енергія "спалюється" в досвід
                        if (accepted < extracted) {
                            pEnergy.addExperience((extracted - accepted) * 0.02f, serverPlayer);
                        }
                        changed = true;
                    }

                    // Синхронізація кожні 10 тіків
                    if (level.getGameTime() % 10 == 0) pEnergy.sync(serverPlayer);
                }
            }
        }

        if (changed) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
    private void processEnergyTransfer(Level level, BlockPos pos, @Nullable ServerPlayer player) {
        // Тепер цей метод не залежить від того, чи є гравець у циклі
        if (storedEnergy <= 0) return;

        for (Direction dir : Direction.values()) {
            if (storedEnergy <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null || neighbor instanceof WirelessEnergyInterfaceBlockEntity) continue;

            // Передача в Тх мережі
            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(txCap -> {
                int accepted = txCap.receiveTacionEnergy(Math.min(storedEnergy, 100), false);
                if (accepted > 0) this.extractTacionEnergy(accepted, false);
            });

            // Конвертація в RF (Досвід даємо тільки якщо player != null)
            if (storedEnergy > 0) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(rfCap -> {
                    if (rfCap.canReceive()) {
                        int acceptedRF = rfCap.receiveEnergy(Math.min(storedEnergy * 10, 1000), false);
                        int usedTx = (int) Math.ceil(acceptedRF / 10.0);
                        if (usedTx > 0) {
                            this.extractTacionEnergy(usedTx, false);

                            // Даємо досвід гравцю, якщо він поруч
                            if (player != null) {
                                player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY)
                                        .ifPresent(e -> e.addExperience(usedTx * 0.15f, player));
                            }
                        }
                    }
                });
            }
        }
    }

    public int receiveFromPlayer(int amount, boolean simulate) {
        int space = MAX_CAPACITY - storedEnergy;
        int toAdd = Math.min(amount, space);
        if (!simulate && toAdd > 0) { storedEnergy += toAdd; setChanged(); }
        return toAdd;
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) { return 0; } // Тільки від гравців
    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int toTake = Math.min(storedEnergy, amount);
        if (!simulate && toTake > 0) { storedEnergy -= toTake; setChanged(); }
        return toTake;
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return tachyonHolder.cast();
        if (cap == ForgeCapabilities.ENERGY) return rfHolder.cast();
        return super.getCapability(cap, side);
    }

    @Override protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("StoredEnergy", storedEnergy);
        nbt.putInt("Mode", mode);
    }

    @Override public void load(CompoundTag nbt) {
        super.load(nbt);
        this.storedEnergy = nbt.getInt("StoredEnergy");
        this.mode = nbt.getInt("Mode");
    }
}