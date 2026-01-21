package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class EnergyColorHelper {
    public static int getColor() {
        long time = Minecraft.getInstance().level.getGameTime();
        float partialTick = Minecraft.getInstance().getFrameTime();
        float totalTime = time + partialTick;
        float ratio = ClientPlayerEnergy.getRatio(); // 1.0 = 100%

        // 1. Смертельна небезпека (Швидке тривожне блимання Червоний/Чорний)
        if (ratio > 1.8f) {
            float f = (Mth.sin(totalTime * 0.8f) + 1.0f) * 0.5f;
            return lerpColor(0xFFFF0000, 0xFF000000, f);
        }

        // 2. Сильне перевантаження (Помаранчевий/Червоний)
        if (ratio > 1.5f) {
            float f = (Mth.sin(totalTime * 0.4f) + 1.0f) * 0.5f;
            return lerpColor(0xFFFFA500, 0xFFFF0000, f);
        }

        // 3. Попередження про нестабільність (80%+) - Жовтий колір
        if (ratio >= 0.8f && ratio <= 1.0f) {
            return 0xFFFFFF00;
        }

        // 4. Глушилка
        if (ClientPlayerEnergy.isJammed()) {
            return (time % 10 < 5) ? 0xFFFFFF00 : 0xFF444400; // Швидше блимання
        }

        // Інші стани
        if (ClientPlayerEnergy.isRemoteNoDrain()) {
            float f = (Mth.sin(totalTime * 0.1f) + 1.0f) * 0.5f;
            return lerpColor(0xFF00FFFF, 0xFF0055FF, f);
        }

        if (ClientPlayerEnergy.isOverloaded()) return 0xFFFFA500; // 100%+
        if (ClientPlayerEnergy.isCriticalLow()) return 0xFFFF0000;
        if (ClientPlayerEnergy.isStabilized()) return 0xFF00FF44;
        if (ClientPlayerEnergy.isRemoteStabilized()) return 0xFFA020F0;

        return 0xFF00FFFF;
    }

    private static int lerpColor(int c1, int c2, float t) {
        int r = (int) Mth.lerp(t, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, c1 & 0xFF, c2 & 0xFF);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}