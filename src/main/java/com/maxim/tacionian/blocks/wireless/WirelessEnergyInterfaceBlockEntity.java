package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.List;

public class WirelessEnergyInterfaceBlockEntity extends BlockEntity {
    private int mode = 0; // 0:Safe (75%), 1:Balanced (40%), 2:Performance (15%), 3:Unrestricted (0%)

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
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

        // Визначаємо поріг згідно з режимом
        int threshold = switch (be.mode) {
            case 0 -> 75;
            case 1 -> 40;
            case 2 -> 15;
            default -> 0;
        };

        for (Player player : players) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                    // 1. Фіолетовий худ (зв'язок встановлено)
                    pEnergy.setRemoteStabilized(true);

                    // 2. Перевірка потреб бази
                    boolean machinesNeedPower = checkMachinesNeedPower(level, pos);
                    int currentPercent = pEnergy.getEnergyPercent();

                    if (machinesNeedPower) {
                        // Якщо база потребує енергії — дозволяємо реген гравця (працюємо як генератор)
                        pEnergy.setRemoteNoDrain(false);

                        // Викачуємо тільки якщо енергія вище ліміту режиму
                        if (level.getGameTime() % 10 == 0) {
                            if (currentPercent > threshold) {
                                processEnergyTransfer(level, pos, serverPlayer, pEnergy);
                            }
                            pEnergy.sync(serverPlayer);
                        }
                    } else {
                        // Якщо база заповнена — блокуємо реген вище ліміту режиму (захист від перевантаження)
                        if (currentPercent > threshold) {
                            pEnergy.setRemoteNoDrain(true);
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
                boolean canAccept = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                        .map(cap -> cap.canReceive() && cap.receiveEnergy(10, true) > 0)
                        .orElse(false);
                if (canAccept) return true;
            }
        }
        return false;
    }

    private static void processEnergyTransfer(Level level, BlockPos pos, ServerPlayer serverPlayer, com.maxim.tacionian.energy.PlayerEnergy pEnergy) {
        int txToTakeMax = 150;
        int feNeededMax = txToTakeMax * 10;
        int totalAcceptedByNeighbors = 0;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                final int currentLimit = feNeededMax - totalAcceptedByNeighbors;
                if (currentLimit <= 0) break;
                totalAcceptedByNeighbors += neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                        .map(cap -> cap.receiveEnergy(currentLimit, false))
                        .orElse(0);
            }
        }

        if (totalAcceptedByNeighbors > 0) {
            int txToActuallyExtract = (totalAcceptedByNeighbors + 9) / 10;
            pEnergy.extractEnergyWithExp(txToActuallyExtract, false, serverPlayer);
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

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putInt("Mode", mode);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.mode = nbt.getInt("Mode");
    }
}