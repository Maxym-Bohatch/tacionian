package com.maxim.tacionian.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class StabilizationPlateBlock extends Block implements EntityBlock {
    public StabilizationPlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StabilizationPlateBlockEntity plate) {
                if (player.isShiftKeyDown()) {
                    // Зміна режиму
                    plate.cycleMode();
                    String modeKey = switch (plate.getCurrentMode()) {
                        case 0 -> "mode.tacionian.safe";
                        case 1 -> "mode.tacionian.balanced";
                        case 2 -> "mode.tacionian.performance";
                        default -> "mode.tacionian.unrestricted";
                    };
                    player.displayClientMessage(Component.translatable("message.tacionian.mode_switched",
                            Component.translatable(modeKey)), true);
                } else {
                    // Показ буфера енергії
                    player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy",
                            plate.getEnergy(), plate.getMaxCapacity()), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StabilizationPlateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof StabilizationPlateBlockEntity plate) StabilizationPlateBlockEntity.tick(lvl, pos, st, plate);
        };
    }
}