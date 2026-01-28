/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.maxim.tacionian.energy.control;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyControlResolver {
    public static void resolve(ServerPlayer player, PlayerEnergy energy) {
        // 1. Перевірка стабілізаторів у інвентарі
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                energy.setStabilized(true);

                // Отримуємо режим стабілізатора:
                // 0: Safe (75%), 1: Balanced (40%), 2: Performance (15%), 3: Unrestricted (0%)
                int mode = stack.getOrCreateTag().getInt("Mode");
                int threshold = switch (mode) {
                    case 0 -> 75;
                    case 1 -> 40;
                    case 2 -> 15;
                    default -> 0;
                };

                // Якщо енергія вища за поріг режиму, активуємо "NoDrain" (заборона пасивного витрачання)
                if (energy.getEnergyPercent() > threshold) {
                    energy.setRemoteNoDrain(true);
                }
                break;
            }
        }

        // 2. Перевірка плити під ногами
        BlockPos pos = player.blockPosition().below();
        BlockState state = player.level().getBlockState(pos);
        if (state.is(ModBlocks.STABILIZATION_PLATE.get())) {
            energy.setPlateStabilized(true);

            // Якщо гравець присів на плиті — швидко зливаємо енергію в "землю" (очищення)
            if (player.isCrouching() && energy.getEnergyPercent() > 5) {
                energy.extractEnergyPure(40, false);
                if (player.tickCount % 4 == 0) {
                    player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY(), player.getZ(),
                            3, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        // --- ВИПРАВЛЕННЯ: Жорсткий ліміт видалено ---
        // Раніше тут був код, який скидав енергію на 95%.
        // Тепер ми дозволяємо системі PlayerEnergy.tick самостійно вирішувати,
        // який ліміт використовувати (безпечні 99% чи стабілізовані 199%).
    }
}