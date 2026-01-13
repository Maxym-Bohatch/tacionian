package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class TachyonChargerBlock extends Block {
    private final boolean isSafe;

    public TachyonChargerBlock(Properties props, boolean isSafe) {
        super(props);
        this.isSafe = isSafe;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            int minLimit = isSafe ? (int)(pEnergy.getMaxEnergy() * 0.15f) : 0;
            if (pEnergy.getEnergy() > minLimit) {
                int toConvertTx = 100;
                int totalRFToGive = toConvertTx * 10;
                int distributedRF = 0;

                for (Direction dir : Direction.values()) {
                    BlockEntity be = level.getBlockEntity(pos.relative(dir));
                    if (be != null) {
                        distributedRF += be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                                .map(cap -> cap.receiveEnergy(totalRFToGive / 4, false)).orElse(0);
                    }
                }

                // Просто прибираємо level.getGameTime()
                pEnergy.extractEnergyWithExp(distributedRF / 10, false, player);
            }
        });
        return InteractionResult.SUCCESS;
    }
}