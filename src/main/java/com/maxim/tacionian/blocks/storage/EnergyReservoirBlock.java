/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

public class EnergyReservoirBlock extends BaseEntityBlock {
    public EnergyReservoirBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyReservoirBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RESERVOIR_BE.get(),
                EnergyReservoirBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyReservoirBlockEntity reservoir) {

            // Перевірка порожньої руки для виводу інформації
            if (player.getItemInHand(hand).isEmpty() && !player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy",
                        reservoir.getEnergy(), reservoir.getMaxCapacity()).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            }

            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int amount = 500;
                boolean actionHappened = false;

                if (player.isShiftKeyDown()) {
                    // РЕЖИМ: Витягування з бака в гравця
                    // 1. Питаємо гравця, скільки він реально МОЖЕ прийняти (simulate: true)
                    int canPlayerReceive = pEnergy.receiveEnergyPure(amount, true);
                    if (canPlayerReceive > 0) {
                        // 2. Витягуємо з резервуара рівно стільки, скільки влізе в гравця
                        int actuallyExtracted = reservoir.extractTacionEnergy(canPlayerReceive, false);
                        if (actuallyExtracted > 0) {
                            pEnergy.receiveEnergyPure(actuallyExtracted, false);
                            player.displayClientMessage(Component.translatable("message.tacionian.reservoir_extracted", actuallyExtracted)
                                    .withStyle(ChatFormatting.GREEN), true);
                            actionHappened = true;
                        }
                    }
                } else {
                    // РЕЖИМ: Заливка з гравця в бак
                    // 1. Питаємо бак, скільки він може прийняти (simulate: true)
                    int canReservoirReceive = reservoir.receiveTacionEnergy(amount, true);
                    if (canReservoirReceive > 0) {
                        // 2. Витягуємо з гравця стільки, скільки бак готовий прийняти
                        int actuallyTakenFromPlayer = pEnergy.extractEnergyPure(canReservoirReceive, false);
                        if (actuallyTakenFromPlayer > 0) {
                            reservoir.receiveTacionEnergy(actuallyTakenFromPlayer, false);
                            player.displayClientMessage(Component.translatable("message.tacionian.reservoir_inserted", actuallyTakenFromPlayer)
                                    .withStyle(ChatFormatting.RED), true);
                            actionHappened = true;
                        }
                    }
                }

                if (actionHappened) {
                    pEnergy.sync((ServerPlayer) player);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnergyReservoirBlockEntity reservoir) {
                int leftover = reservoir.getEnergy();
                if (leftover > 0 && !level.isClientSide) {
                    // Викидаємо івент для аддонів (хмари)
                    MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, leftover));

                    // Візуальне розсіювання енергії
                    ((ServerLevel)level).sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            15, 0.3, 0.3, 0.3, 0.1);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}