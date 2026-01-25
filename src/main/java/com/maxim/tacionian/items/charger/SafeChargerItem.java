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

package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SafeChargerItem extends Item {
    public SafeChargerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean active = !stack.getOrCreateTag().getBoolean("Active");
            stack.getOrCreateTag().putBoolean("Active", active);
            player.displayClientMessage(Component.translatable(active ? "status.tacionian.active" : "status.tacionian.disabled"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) return;
        if (!stack.getOrCreateTag().getBoolean("Active")) return;

        // Перевірка кожні 5 тіків для економії ресурсів
        if (level.getGameTime() % 5 == 0) {
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // Безпечний поріг: 15% від максимуму
                int minEnergy = (int) (pEnergy.getMaxEnergy() * 0.15f);
                int currentEnergy = pEnergy.getEnergy();

                if (currentEnergy <= minEnergy) return;

                // Використовуємо масив для можливості модифікації всередині лямбди
                final int[] availableToTake = { currentEnergy - minEnergy };
                boolean changed = false;

                for (ItemStack target : serverPlayer.getInventory().items) {
                    if (target.isEmpty() || target == stack) continue;
                    if (availableToTake[0] <= 0) break;

                    // 1. Пріоритет: Tachyon Energy
                    var txCapOpt = target.getCapability(ModCapabilities.TACHYON_STORAGE);
                    if (txCapOpt.isPresent()) {
                        int txAdded = txCapOpt.map(cap -> {
                            int needed = cap.getMaxCapacity() - cap.getEnergy();
                            if (needed <= 0) return 0;

                            int toGive = Math.min(availableToTake[0], Math.min(needed, 50));
                            int extracted = pEnergy.extractEnergyPure(toGive, false);

                            if (extracted > 0) {
                                pEnergy.addExperience(extracted * 0.12f, serverPlayer);
                                return cap.receiveTacionEnergy(extracted, false);
                            }
                            return 0;
                        }).orElse(0);

                        if (txAdded > 0) {
                            changed = true;
                            availableToTake[0] -= txAdded;
                            continue;
                        }
                    }

                    // 2. Другорядне: RF Energy
                    var rfCapOpt = target.getCapability(ForgeCapabilities.ENERGY);
                    if (rfCapOpt.isPresent()) {
                        int rfAdded = rfCapOpt.map(cap -> {
                            if (!cap.canReceive()) return 0;

                            int maxRF = 500; // Еквівалент 50 Tx
                            int canAcceptRF = cap.receiveEnergy(maxRF, true);

                            if (canAcceptRF > 0) {
                                int txNeeded = (int) Math.ceil(canAcceptRF / 10.0);
                                int toGive = Math.min(availableToTake[0], Math.min(txNeeded, 50));

                                int extracted = pEnergy.extractEnergyPure(toGive, false);
                                if (extracted > 0) {
                                    pEnergy.addExperience(extracted * 0.18f, serverPlayer);
                                    return cap.receiveEnergy(extracted * 10, false);
                                }
                            }
                            return 0;
                        }).orElse(0);

                        if (rfAdded > 0) {
                            changed = true;
                            availableToTake[0] -= (rfAdded / 10);
                        }
                    }
                }

                if (changed) {
                    pEnergy.sync(serverPlayer);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                                serverPlayer.getX(), serverPlayer.getY() + 1.2, serverPlayer.getZ(),
                                1, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            });
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean("Active");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean active = stack.getOrCreateTag().getBoolean("Active");
        tooltip.add(Component.translatable("item.tacionian.safe_charger_item").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tacionian.safe_charger_item.desc").withStyle(ChatFormatting.GRAY));

        String statusKey = active ? "status.tacionian.active" : "status.tacionian.disabled";
        tooltip.add(Component.translatable(statusKey)
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}