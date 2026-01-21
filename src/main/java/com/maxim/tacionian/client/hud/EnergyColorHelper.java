package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class EnergyColorHelper {
    public static int getColor() {
        long time = Minecraft.getInstance().level.getGameTime();
        float totalTime = time + Minecraft.getInstance().getFrameTime();

        if (ClientPlayerEnergy.isCriticalOverload()) {
            float pulse = (Mth.sin(totalTime * 1.5f) + 1.0f) * 0.5f;
            return lerpColor(0xFF00FBFF, 0xFFFFFFFF, pulse);
        }

        if (ClientPlayerEnergy.getCustomColor() != -1) {
            return ClientPlayerEnergy.getCustomColor();
        }

        if (ClientPlayerEnergy.isRemoteStabilized() || ClientPlayerEnergy.isRemoteNoDrain()) {
            return 0xFFA020F0; // Фіолетовий
        }

        if (ClientPlayerEnergy.isStabilized()) return 0xFF00FF44; // Зелений
        if (ClientPlayerEnergy.isOverloaded()) return 0xFFFFFF00; // Жовтий
        if (ClientPlayerEnergy.isJammed()) return 0xFFFF0000;
        if (ClientPlayerEnergy.isCriticalLow()) return 0xFF550000;

        return 0xFF00FFFF; // Блакитний
    }

    public static float getShakeAmplitude() {
        float ratio = ClientPlayerEnergy.getRatio();

        // Визначаємо поріг тряски: 96% для новачків, 80% для профі
        float threshold = (ClientPlayerEnergy.getLevel() <= 5) ? 0.96f : 0.8f;

        // Якщо енергія нижче порогу АБО діє стабілізація — тряски НЕМАЄ
        if (ratio < threshold || ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) {
            return 0f;
        }

        // Розрахунок сили тряски (починається плавно від порогу)
        return (ratio - threshold) * 25.0f;
    }

    private static int lerpColor(int c1, int c2, float t) {
        int r = (int) Mth.lerp(t, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, c1 & 0xFF, c2 & 0xFF);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}