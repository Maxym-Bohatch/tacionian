package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class EnergyColorHelper {
    public static int getColor() {
        long time = Minecraft.getInstance().level.getGameTime();
        float partialTick = Minecraft.getInstance().getFrameTime();
        float totalTime = time + partialTick;
        float ratio = ClientPlayerEnergy.getRatio();

        // 1. Смертельна небезпека (М'яке тривожне блимання)
        if (ratio > 1.5f) {
            float f = (Mth.sin(totalTime * 0.4f) + 1.0f) * 0.5f;
            return lerpColor(0xFFFFA500, 0xFFFF0000, f); // Помаранчевий -> Червоний
        }

        // 2. Глушилка
        if (ClientPlayerEnergy.isJammed()) {
            return (time % 20 < 10) ? 0xFFFFFF00 : 0xFF666600; // Повільніше жовте блимання
        }

        // 3. Активна дистанційна передача (Плавне переливання Cyan -> Deep Blue)
        if (ClientPlayerEnergy.isRemoteNoDrain()) {
            float f = (Mth.sin(totalTime * 0.1f) + 1.0f) * 0.5f;
            return lerpColor(0xFF00FFFF, 0xFF0055FF, f);
        }

        if (ClientPlayerEnergy.isOverloaded()) return 0xFFFFA500;
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