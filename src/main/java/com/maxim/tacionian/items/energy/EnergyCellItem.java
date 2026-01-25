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

package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModCapabilities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class EnergyCellItem extends Item {
    private static final int MAX_ENERGY = 3000;

    public EnergyCellItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (be != null) {
            return be.getCapability(ModCapabilities.TACHYON_STORAGE, context.getClickedFace()).map(storage -> {
                CompoundTag nbt = stack.getOrCreateTag();
                int currentEnergy = nbt.getInt("energy");

                if (player != null && player.isShiftKeyDown()) {
                    int toGive = storage.receiveTacionEnergy(currentEnergy, false);
                    nbt.putInt("energy", currentEnergy - toGive);
                    player.displayClientMessage(Component.translatable("message.tacionian.discharged", toGive).withStyle(ChatFormatting.RED), true);
                    // ЗВУК: Твоя розрядка в блок
                    level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.7f, 0.8f);
                } else {
                    int space = MAX_ENERGY - currentEnergy;
                    int toTake = storage.extractTacionEnergy(space, false);
                    nbt.putInt("energy", currentEnergy + toTake);
                    if (player != null) {
                        player.displayClientMessage(Component.translatable("message.tacionian.charged", toTake).withStyle(ChatFormatting.GREEN), true);
                        // ЗВУК: Твоя зарядка в предмет
                        level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.7f, 1.3f);
                    }
                }
                return InteractionResult.CONSUME;
            }).orElse(InteractionResult.PASS);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);

        serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            CompoundTag nbt = stack.getOrCreateTag();
            int stored = nbt.getInt("energy");
            int step = 100;

            if (serverPlayer.isShiftKeyDown()) {
                int toGive = Math.min(Math.min(stored, step), pEnergy.getMaxEnergy() - pEnergy.getEnergy());
                if (toGive > 0) {
                    pEnergy.receiveEnergyPure(toGive, false);
                    nbt.putInt("energy", stored - toGive);
                    // ЗВУК: Ефект переливання в гравця
                    level.playSound(null, player.blockPosition(), ModSounds.MODE_SWITCH.get(), SoundSource.PLAYERS, 0.4f, 1.2f);
                }
            } else {
                int spaceInCell = MAX_ENERGY - stored;
                int toTake = Math.min(Math.min(pEnergy.getEnergy(), step), spaceInCell);
                if (toTake > 0) {
                    pEnergy.extractEnergyPure(toTake, false);
                    nbt.putInt("energy", stored + toTake);
                    // ЗВУК: Ефект переливання в комірку
                    level.playSound(null, player.blockPosition(), ModSounds.MODE_SWITCH.get(), SoundSource.PLAYERS, 0.4f, 0.8f);
                }
            }
            pEnergy.sync(serverPlayer);
        });
        return InteractionResultHolder.success(stack);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) {
        int energy = stack.hasTag() ? stack.getTag().getInt("energy") : 0;
        return Math.round((float) energy * 13.0F / (float) MAX_ENERGY);
    }
    @Override public int getBarColor(ItemStack stack) { return Mth.hsvToRgb(0.55F, 1.0F, 1.0F); }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int energy = stack.hasTag() ? stack.getTag().getInt("energy") : 0;
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.charge", energy, MAX_ENERGY).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.block_hint").withStyle(ChatFormatting.GRAY));
    }
}