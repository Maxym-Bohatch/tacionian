/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
import net.minecraftforge.common.MinecraftForge;
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
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StabilizationPlateBlockEntity plate) {
                int leftover = plate.getEnergy();
                if (leftover > 0 && !level.isClientSide) {
                    MinecraftForge.EVENT_BUS.post(new com.maxim.tacionian.api.events.TachyonWasteEvent(level, pos, leftover));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}