package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class EnergyHudOverlay {
    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.isSpectator()) return;
        if (!ClientPlayerEnergy.hasData()) return;

        // --- КООРДИНАТИ ТА РОЗМІРИ ---
        int x = 15;
        int y = 15;
        int barWidth = 120; // Трохи ширше для кращого вигляду
        int barHeight = 8;
        float gameTime = mc.level.getGameTime() + partialTick;

        // --- ЛОГІКА СТАНІВ ТА КОЛЬОРІВ ---
        int baseColor;
        boolean isDanger = false;
        boolean isStable = false;
        boolean isRemote = ClientPlayerEnergy.isRemoteStabilized();

        if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.isCriticalLow()) {
            baseColor = 0xFFFF4444; // Червоний
            isDanger = true;
        } else if (ClientPlayerEnergy.isStabilized()) {
            baseColor = 0xFF44FF44; // Зелений
            isStable = true;
        } else if (isRemote) {
            baseColor = 0xFFAA44FF; // Фіолетовий
            isStable = true;
        } else {
            baseColor = 0xFF00A0FF; // Блакитний
        }

        // Анімація кольору
        int finalColor = baseColor;
        if (isDanger) {
            float pulse = (float) (Math.sin(gameTime * 0.4f) + 1.0f) * 0.5f * 0.3f + 0.7f;
            finalColor = multiplyColor(baseColor, pulse);
        } else if (isStable) {
            float breath = (float) (Math.sin(gameTime * 0.1f) + 1.0f) * 0.5f * 0.2f + 0.8f;
            finalColor = multiplyColor(baseColor, breath);
        }

        // --- РЕНДЕР ТЕКСТУ В СТОВПЧИК ---
        // Рядок 1: Рівень ядра
        Component lvlComp = Component.translatable("tooltip.tacionian.level");
        String levelText = lvlComp.getString() + ": " + ClientPlayerEnergy.getLevel();
        graphics.drawString(mc.font, levelText, x, y, isDanger ? 0xFFFFAA00 : 0xFFFFFF, true);

        // Рядок 2: Поточна / Максимальна енергія
        // Використовуємо getMaxEnergy() з логіки, щоб показати ліміт
        String energyValues = ClientPlayerEnergy.getEnergy() + " / " + ClientPlayerEnergy.getMaxEnergy() + " Tx";
        graphics.drawString(mc.font, energyValues, x, y + 10, 0xAAAAAA, true);

        // --- РЕНДЕР БАРІВ ---
        int barY = y + 22; // Зміщуємо нижче, бо тепер два рядки тексту
        float ratio = Math.min(ClientPlayerEnergy.getRatio(), 1.0f);
        int filledWidth = (int) (barWidth * ratio);

        // Рамка та фон
        graphics.fill(x - 1, barY - 1, x + barWidth + 1, barY + barHeight + 1, 0xFF000000);
        graphics.fill(x, barY, x + barWidth, barY + barHeight, 0xFF111111);

        if (filledWidth > 0) {
            graphics.fill(x, barY, x + filledWidth, barY + barHeight, finalColor);

            // Статичний відблиск зверху
            graphics.fill(x, barY, x + filledWidth, barY + 2, 0x33FFFFFF);

            // Анімований "сканер" для стабільних станів
            if (isStable) {
                float scanPos = (gameTime % 40) / 40.0f;
                int scanX = x + (int)(filledWidth * scanPos);
                if (scanX < x + filledWidth) {
                    graphics.fill(scanX, barY, Math.min(scanX + 3, x + filledWidth), barY + barHeight, 0x44FFFFFF);
                }
            }
        }

        // Рядок 3: Бар досвіду (Золотий)
        int xpY = barY + barHeight + 3;
        float expRatio = Math.max(0, Math.min((float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp(), 1.0f));
        int xpFilledWidth = (int) (barWidth * expRatio);

        if (xpFilledWidth > 0) {
            graphics.fill(x, xpY, x + barWidth, xpY + 1, 0xFF000000);
            graphics.fill(x, xpY, x + xpFilledWidth, xpY + 1, 0xFFFFD700);
        }
    };

    private static int multiplyColor(int color, float multiplier) {
        int r = (int) (((color >> 16) & 0xFF) * multiplier);
        int g = (int) (((color >> 8) & 0xFF) * multiplier);
        int b = (int) ((color & 0xFF) * multiplier);
        return (0xFF << 24) | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }
}