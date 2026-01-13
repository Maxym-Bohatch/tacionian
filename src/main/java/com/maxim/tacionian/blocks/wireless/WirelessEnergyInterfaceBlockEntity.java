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
    private int mode = 0; // 0:Safe, 1:Balanced, 2:Performance, 3:Unrestricted

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
        if (level.isClientSide || level.getGameTime() % 10 != 0) return;

        AABB area = new AABB(pos).inflate(10);
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

                    if (pEnergy.getEnergyPercent() > threshold) {
                        int txToTake = 20;
                        int extractedTx = pEnergy.extractEnergyWithExp(txToTake, false, serverPlayer);

                        if (extractedTx > 0) {
                            int remainingFE = extractedTx * 10;

                            // Розподіляємо енергію між сусідами
                            for (Direction dir : Direction.values()) {
                                if (remainingFE <= 0) break;

                                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                                if (neighbor != null) {
                                    final int toSend = remainingFE;
                                    int accepted = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                                            .map(cap -> cap.canReceive() ? cap.receiveEnergy(toSend, false) : 0)
                                            .orElse(0);
                                    remainingFE -= accepted;
                                }
                            }
                            // Обов'язкова синхронізація для HUD
                            pEnergy.sync(serverPlayer);
                        }
                    }
                });
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