package com.maxim.tacionian.blocks;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StabilizationPlateBlock extends PressurePlateBlock {
    // Форми: (X1, Y1, Z1, X2, Y2, Z2) в пікселях (0-16)
    // Робимо блок товщиною в 2 пікселі, щоб він був помітним, але плоским
    protected static final VoxelShape SHAPE_IDLE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    protected static final VoxelShape SHAPE_POWERED = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public StabilizationPlateBlock(Properties props) {
        super(Sensitivity.EVERYTHING, props, BlockSetType.IRON);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(POWERED) ? SHAPE_POWERED : SHAPE_IDLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                int toDrain = Math.min(energy.getEnergy(), 500);
                int drained = energy.extractEnergyPure(toDrain, false, level.getGameTime());

                if (drained > 0) {
                    player.displayClientMessage(Component.translatable("message.tacionian.energy_drained_air", drained), true);
                }
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide && entity instanceof Player player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> energy.setStabilized(true));
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> energy.setStabilized(true));
        }
        super.stepOn(level, pos, state, entity);
    }
}