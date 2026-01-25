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

package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class EnergyColorHelper {
    public static int getColor() {
        float ratio = ClientPlayerEnergy.getRatio();
        long time = Minecraft.getInstance().level.getGameTime();

        // 1. Дефіцит енергії (менше 10%) - Пульсуючий червоний
        if (ratio < 0.10f) {
            float pulse = (Mth.sin(time * 0.4f) + 1f) * 0.5f;
            return lerpColor(0xFF440000, 0xFFFF0000, pulse); // Від темно-червоного до яскравого
        }

        // 2. Стабілізація (Пріоритет захисту)
        if (ClientPlayerEnergy.isRemoteNoDrain()) return 0xFF00FF44; // Зелений
        if (ClientPlayerEnergy.isPlateStabilized()) return 0xFF2B57FF; // Синій
        if (ClientPlayerEnergy.isInterfaceStabilized()) return 0xFFAA00FF; // Фіолетовий

        // 3. Стан Новачка (рівні 1-5) - Світло-блакитний (Safe)
        if (ClientPlayerEnergy.getLevel() >= 1 && ClientPlayerEnergy.getLevel() <= 5) return 0xFF00FBFF;

        // 4. Перевантаження (більше 98%) - Пульсуючий біло-червоний (Критично)
        if (ratio > 0.98f) {
            float pulse = (Mth.sin(time * 0.5f) + 1f) * 0.5f;
            return lerpColor(0xFFFF0000, 0xFFFFFFFF, pulse);
        }

        // 5. Перед-критичний стан (більше 85%) - Помаранчевий
        if (ratio > 0.85f) {
            return 0xFFFF8800;
        }

        // 6. Стандартний колір (Aqua)
        return 0xFF00FFFF;
    }

    public static float getShakeAmplitude() {
        // Додаємо тряску і при дефіциті!
        float ratio = ClientPlayerEnergy.getRatio();
        if (Minecraft.getInstance().player.isCreative()) return 0f;

        // Тряска при перевантаженні
        if (ratio > 0.85f && !isAnyProtection()) {
            return (ratio - 0.85f) * 15.0f;
        }

        // Тряска при дефіциті (енергія нестабільна)
        if (ratio < 0.10f) {
            return (0.10f - ratio) * 20.0f;
        }

        return 0f;
    }

    private static boolean isAnyProtection() {
        return ClientPlayerEnergy.isRemoteNoDrain() ||
                ClientPlayerEnergy.isPlateStabilized() ||
                ClientPlayerEnergy.isInterfaceStabilized() ||
                ClientPlayerEnergy.getLevel() <= 5;
    }

    public static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpColor(int c1, int c2, float t) {
        int r = (int) Mth.lerp(t, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, c1 & 0xFF, c2 & 0xFF);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}