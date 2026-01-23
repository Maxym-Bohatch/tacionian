package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class EnergyColorHelper {
    public static int getColor() {
        if (!ClientPlayerEnergy.hasData()) return 0xFF00FFFF;

        float ratio = ClientPlayerEnergy.getRatio();
        long time = Minecraft.getInstance().level.getGameTime();
        float totalTime = time + Minecraft.getInstance().getFrameTime();

        // Блимання при перевантаженні без захисту
        if (ratio > 0.98f && !isAnyProtection()) {
            float pulse = (Mth.sin(totalTime * 1.5f) + 1.0f) * 0.5f;
            return lerpColor(0xFFFF0000, 0xFFFFFFFF, pulse);
        }

        if (ClientPlayerEnergy.isRemoteNoDrain()) return 0xFF00FF44; // Зелений (Стабілізатор)
        if (ClientPlayerEnergy.isInterfaceStabilized()) return 0xFFA020F0; // Фіолетовий
        if (ClientPlayerEnergy.isPlateStabilized()) return 0xFF00FBFF; // Блакитний

        if (ratio > 0.85f) return 0xFFFFFF00; // Жовтий
        return 0xFF00FFFF; // Стандарт
    }

    private static boolean isAnyProtection() {
        return ClientPlayerEnergy.isRemoteNoDrain() || ClientPlayerEnergy.isInterfaceStabilized() || ClientPlayerEnergy.isPlateStabilized();
    }

    public static float getShakeAmplitude() {
        float ratio = ClientPlayerEnergy.getRatio();
        if (ratio < 0.8f || isAnyProtection()) return 0f;
        return (ratio - 0.8f) * 20.0f;
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