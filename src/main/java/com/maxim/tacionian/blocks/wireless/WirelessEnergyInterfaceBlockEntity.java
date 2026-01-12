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
    private static final double RANGE = 10.0;
    private int mode = 1; // 0: 30%, 1: 60%, 2: 90%

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    public void cycleMode(Player player) {
        this.mode = (this.mode + 1) % 3;
        int percent = mode == 0 ? 30 : mode == 1 ? 60 : 90;

        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            pEnergy.setStabilizationThreshold(percent);
        });

        player.sendSystemMessage(Component.literal("§b[Інтерфейс] §7Поріг встановлено: §f" + percent + "%"));
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessEnergyInterfaceBlockEntity be) {
        if (level.isClientSide) return;

        int thresholdPercent = be.mode == 0 ? 30 : be.mode == 1 ? 60 : 90;
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(RANGE));
        long time = level.getGameTime();

        for (ServerPlayer player : players) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                pEnergy.setRemoteStabilized(true);
                pEnergy.setStabilizationThreshold(thresholdPercent); // Передаємо налаштування в ядро

                int limit = (int)(pEnergy.getMaxEnergy() * (thresholdPercent / 100.0f));

                if (pEnergy.getEnergy() < limit) {
                    pEnergy.receiveEnergy(20, false);
                }

                if (pEnergy.getEnergy() >= limit) {
                    for (Direction dir : Direction.values()) {
                        BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                        if (neighbor != null) {
                            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(rf -> {
                                int toSend = Math.min(pEnergy.getEnergy() - limit, 100);
                                if (toSend > 0) {
                                    int accepted = rf.receiveEnergy(toSend, false);
                                    pEnergy.extractEnergyPure(accepted, false, time);
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    @Override protected void saveAdditional(CompoundTag nbt) { nbt.putInt("StabilizationMode", mode); super.saveAdditional(nbt); }
    @Override public void load(CompoundTag nbt) { super.load(nbt); this.mode = nbt.getInt("StabilizationMode"); }
}