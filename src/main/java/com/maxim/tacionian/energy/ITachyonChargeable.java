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

package com.maxim.tacionian.energy;

import net.minecraft.world.item.ItemStack;

public interface ITachyonChargeable {
    int getMaxEnergy(ItemStack stack);
    int getEnergy(ItemStack stack);
    int receiveEnergy(ItemStack stack, int amount, boolean simulate);
    int extractEnergy(ItemStack stack, int amount, boolean simulate);

    /** Чи заповнений предмет повністю */
    default boolean isFull(ItemStack stack) {
        return getEnergy(stack) >= getMaxEnergy(stack);
    }

    /** Чи є в предметі енергія для виконання дії */
    default boolean hasEnergy(ItemStack stack, int amount) {
        return getEnergy(stack) >= amount;
    }

    /** Повертає рівень заряду (0.0 - 1.0) для малювання смужки міцності або HUD */
    default float getChargeLevel(ItemStack stack) {
        return getMaxEnergy(stack) > 0 ? (float) getEnergy(stack) / getMaxEnergy(stack) : 0.0f;
    }
}