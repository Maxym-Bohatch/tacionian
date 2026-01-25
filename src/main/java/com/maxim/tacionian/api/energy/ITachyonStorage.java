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

package com.maxim.tacionian.api.energy;

public interface ITachyonStorage {
    // Додав "Tacion", як того вимагають твої BlockEntity
    int receiveTacionEnergy(int amount, boolean simulate);
    int extractTacionEnergy(int amount, boolean simulate);
    int getEnergy();
    int getMaxCapacity();

    /** Чи може пристрій приймати енергію зараз */
    default boolean canReceive() {
        return getEnergy() < getMaxCapacity();
    }

    /** Чи може пристрій віддавати енергію */
    default boolean canExtract() {
        return getEnergy() > 0;
    }

    /** Повертає відношення заповненості від 0.0 до 1.0 */
    default float getFillRatio() {
        return getMaxCapacity() > 0 ? (float) getEnergy() / getMaxCapacity() : 0.0f;
    }
}