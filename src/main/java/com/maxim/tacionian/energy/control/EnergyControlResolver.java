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
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                energy.setStabilized(true);
                int mode = stack.getOrCreateTag().getInt("Mode");
                int threshold = (mode == 0) ? 75 : (mode == 1) ? 40 : (mode == 2) ? 15 : 0;
                if (energy.getEnergyPercent() > threshold) energy.setRemoteNoDrain(true);
                break;
            }
        }

        BlockPos pos = player.blockPosition().below();
        BlockState state = player.level().getBlockState(pos);
        if (state.is(ModBlocks.STABILIZATION_PLATE.get())) {
            energy.setPlateStabilized(true); // ВИПРАВЛЕНО
            if (player.isCrouching() && energy.getEnergyPercent() > 5) {
                energy.extractEnergyPure(40, false);
                if (player.tickCount % 4 == 0) {
                    player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY(), player.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        boolean isAnyStabilized = energy.isStabilized() || energy.isInterfaceStabilized() || energy.isPlateStabilized();
        if (isAnyStabilized && energy.isOverloaded()) {
            energy.setEnergy((int)(energy.getMaxEnergy() * 0.95f));
        }
    }
}