package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class EnergyColorHelper {
    public static int getColor() {
        float ratio = ClientPlayerEnergy.getRatio();
        long time = Minecraft.getInstance().level.getGameTime();

        // Тепер використовуємо універсальну перевірку стабілізації
        boolean isSafe = ClientPlayerEnergy.getLevel() <= 5 ||
                ClientPlayerEnergy.isStabilizedLogicActive();

        // 1. Критичне перевантаження (Перед вибухом) - Пульс Білий/Червоний
        float critLimit = isSafe ? 1.98f : 1.02f;
        if (ratio > critLimit) {
            float pulse = (Mth.sin(time * 0.5f) + 1f) * 0.5f;
            return lerpColor(0xFFFF0000, 0xFFFFFFFF, pulse);
        }

        // 2. Специфічні кольори активних режимів (Пріоритет стаціонарним)
        if (ClientPlayerEnergy.isPlateStabilized()) return 0xFF2B57FF;      // Синій
        if (ClientPlayerEnergy.isInterfaceStabilized()) return 0xFFAA00FF;  // Фіолетовий
        if (ClientPlayerEnergy.isRemoteNoDrain()) return 0xFF00FF44;        // Зелений
        if (ClientPlayerEnergy.getLevel() <= 5) return 0xFF00FBFF;          // Блакитний (Новачок)

        // 3. Перед-критичний стан (Оранжевий)
        float warnLimit = isSafe ? 1.80f : 0.88f;
        if (ratio > warnLimit) return 0xFFFF8800;

        // 4. Дефіцит (Червоний пульс)
        if (ratio < 0.10f) {
            float pulse = (Mth.sin(time * 0.4f) + 1f) * 0.5f;
            return lerpColor(0xFF440000, 0xFFFF0000, pulse);
        }

        return 0xFF00FFFF; // Aqua за замовчуванням
    }

    // НОВИЙ МЕТОД: Пульсація для поломаного стабілізатора
    public static int getStabilizerAlertColor(float integrity) {
        if (integrity > 0.4f) return 0xAA000000; // Звичайний фон

        long time = Minecraft.getInstance().level.getGameTime();
        // Плавна пульсація від прозорого до 50% червоного
        float pulse = (Mth.sin(time * 0.2f) + 1f) * 0.5f;
        return lerpColor(0xAA000000, 0xAAFF0000, pulse);
    }

    public static float getShakeAmplitude() {
        float ratio = ClientPlayerEnergy.getRatio();
        if (Minecraft.getInstance().player.isCreative()) return 0f;

        // Тряска починається лише якщо немає активного захисту
        if (ratio > 0.88f && !ClientPlayerEnergy.isStabilizedLogicActive()) {
            return (ratio - 0.88f) * 15.0f;
        }

        if (ratio < 0.10f) return (0.10f - ratio) * 20.0f;

        return 0f;
    }

    public static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF; int a2 = (c2 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF; int r2 = (c2 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;  int g2 = (c2 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;         int b2 = c2 & 0xFF;

        int a = (int) Mth.lerp(t, a1, a2);
        int r = (int) Mth.lerp(t, r1, r2);
        int g = (int) Mth.lerp(t, g1, g2);
        int b = (int) Mth.lerp(t, b1, b2);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}