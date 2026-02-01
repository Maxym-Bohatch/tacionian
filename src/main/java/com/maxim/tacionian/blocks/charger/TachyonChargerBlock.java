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

package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlock extends BaseEntityBlock {
    protected final boolean isSafe;

    public TachyonChargerBlock(BlockBehaviour.Properties props, boolean isSafe) {
        super(props);
        this.isSafe = isSafe;
    }

    public TachyonChargerBlock(BlockBehaviour.Properties props) { this(props, false); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isSafe ? new TachyonSafeChargerBlockEntity(pos, state) : new TachyonChargerBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CHARGER_BE.get(), TachyonChargerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TachyonChargerBlockEntity charger) {
            if (player.isShiftKeyDown()) {
                charger.handlePlayerExtraction(serverPlayer);
                return InteractionResult.SUCCESS;
            }

            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int minLimit = isSafe ? (int)(pEnergy.getMaxEnergy() * 0.15f) : 0;

                if (pEnergy.getEnergy() > minLimit) {
                    int canAccept = charger.receiveTacionEnergy(100, true);
                    if (canAccept > 0) {
                        int available = pEnergy.getEnergy() - minLimit;
                        int toTransfer = Math.min(canAccept, available);

                        if (toTransfer > 0) {
                            int taken = pEnergy.extractEnergyPure(toTransfer, false);
                            charger.receiveTacionEnergy(taken, false);
                            pEnergy.addExperience(taken * 0.1f, serverPlayer);
                            pEnergy.sync(serverPlayer);

                            level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.7f, 1.4f);


                            player.displayClientMessage(Component.translatable("message.tacionian.charged", charger.getEnergy()), true);
                        }
                    } else {

                        player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy", charger.getEnergy()), true);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("message.tacionian.safety_limit").withStyle(ChatFormatting.RED), true);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 1.0f, 0.5f);
                }
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TachyonChargerBlockEntity charger) {
                int leftover = charger.getEnergy();
                if (leftover > 0 && !level.isClientSide) {
                    MinecraftForge.EVENT_BUS.post(new com.maxim.tacionian.api.events.TachyonWasteEvent(level, pos, leftover));

                    ((net.minecraft.server.level.ServerLevel)level).sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            10, 0.2, 0.2, 0.2, 0.05);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}