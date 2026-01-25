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

package com.maxim.tacionian.api.effects;

import com.maxim.tacionian.energy.PlayerEnergy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;

public interface ITachyonEffect {
    /** Колір іконки в HUD */
    int getIconColor();

    /** Чи відображати ефект у спеціальному тахіонному HUD */
    boolean shouldShowInHud();

    /** Шлях до текстури іконки */
    ResourceLocation getIcon(MobEffectInstance instance);

    /** Колір частинок ефекту */
    default int getEffectColor() { return 0xFFFFFF; }

    /** * Основна логіка ефекту. Викликається кожного тіку.
     * Тут аддони можуть прописувати setPhysicsDisabled(true) або змінювати енергію гравця.
     */
    default void applyEffectLogic(ServerPlayer player, PlayerEnergy energy, int amplifier) {
        // Логіка за замовчуванням порожня
    }

    /** * Якщо true, ефект неможливо зняти молоком.
     * Тахіонні зміни структури зазвичай не лікуються молоком.
     */
    default boolean isPersistent() {
        return true;
    }
}