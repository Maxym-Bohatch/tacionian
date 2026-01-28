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

package com.maxim.tacionian.register;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public class ModDamageSources {
    // Змінюємо "energy" на "tachyon_collapse"
    public static final ResourceKey<DamageType> TACHYON_ENERGY =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "tachyon_collapse"));

    public static DamageSource getTachyonDamage(Level level) {
        var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);

        // Шукаємо наш ключ, якщо його немає — беремо стандартний GENERIC_KILL
        Holder<DamageType> holder = registry.getHolder(TACHYON_ENERGY).orElseGet(() ->
                registry.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
        );

        return new DamageSource(holder);

    }
}