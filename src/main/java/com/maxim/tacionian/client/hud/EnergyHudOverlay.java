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

        // --- КОНФІГУРАЦІЯ ---
        int x = 10;
        int y = 10;
        int barWidth = 100;
        int barHeight = 8;
        float gameTime = mc.level.getGameTime() + partialTick;

        // --- ВИЗНАЧЕННЯ СТАНУ ТА КОЛЬОРУ ---
        int baseColor;
        boolean isDanger = false;
        boolean isStable = false;
        boolean isRemote = ClientPlayerEnergy.isRemoteStabilized();

        // Логіка кольорів:
        if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.isCriticalLow()) {
            baseColor = 0xFFFF4444; // Червоний
            isDanger = true;
        } else if (ClientPlayerEnergy.isStabilized()) {
            baseColor = 0xFF44FF44; // Зелений (Предмет в руках/інвентарі)
            isStable = true;
        } else if (isRemote) {
            baseColor = 0xFFAA44FF; // Фіолетовий (Дистанційна дія інтерфейсу/плити)
            isStable = true;
        } else {
            baseColor = 0xFF00A0FF; // Блакитний (Норма)
        }

        // --- ОБРОБКА АНІМАЦІЙ ---
        int finalColor = baseColor;

        if (isDanger) {
            // Швидка тривожна пульсація
            float pulse = (float) (Math.sin(gameTime * 0.4f) + 1.0f) * 0.5f * 0.3f + 0.7f;
            finalColor = multiplyColor(baseColor, pulse);
        } else if (isStable) {
            // М'яке "дихання" для зеленого або фіолетового
            float breath = (float) (Math.sin(gameTime * 0.1f) + 1.0f) * 0.5f * 0.2f + 0.8f;
            finalColor = multiplyColor(baseColor, breath);
        }

        // --- РЕНДЕР ТЕКСТУ (ЛОКАЛІЗАЦІЯ) ---
        Component lvlComp = Component.translatable("tooltip.tacionian.level");
        String levelText = lvlComp.getString() + " " + ClientPlayerEnergy.getLevel();
        String energyText = ClientPlayerEnergy.getEnergy() + " Tx";

        graphics.drawString(mc.font, levelText, x, y, isDanger ? 0xFFFFAA00 : 0xFFFFFF, true);
        int energyTextWidth = mc.font.width(energyText);
        graphics.drawString(mc.font, energyText, x + barWidth - energyTextWidth, y, 0xCCCCCC, true);

        // --- РЕНДЕР БАРІВ ---
        int barY = y + 10;
        float ratio = Math.min(ClientPlayerEnergy.getRatio(), 1.0f);
        int filledWidth = (int) (barWidth * ratio);

        // Контур та фон
        graphics.fill(x - 1, barY - 1, x + barWidth + 1, barY + barHeight + 1, 0xFF000000);
        graphics.fill(x, barY, x + barWidth, barY + barHeight, 0xFF111111);

        if (filledWidth > 0) {
            // Малюємо основну смужку з анімованим кольором
            graphics.fill(x, barY, x + filledWidth, barY + barHeight, finalColor);

            // Глянцевий відблиск (Static highlight)
            graphics.fill(x, barY, x + filledWidth, barY + 2, 0x33FFFFFF);

            // Ефект "Світлової хвилі" (Shimmer) для стабільного стану
            if (isStable) {
                float scanPos = (gameTime % 40) / 40.0f;
                int scanX = x + (int)(filledWidth * scanPos);
                if (scanX < x + filledWidth) {
                    graphics.fill(scanX, barY, Math.min(scanX + 3, x + filledWidth), barY + barHeight, 0x55FFFFFF);
                }
            }
        }

        // Бар досвіду (Золотий)
        int xpY = barY + barHeight + 2;
        float expRatio = Math.max(0, Math.min((float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp(), 1.0f));
        int xpFilledWidth = (int) (barWidth * expRatio);
        if (xpFilledWidth > 0) {
            graphics.fill(x, xpY, x + barWidth, xpY + 1, 0xFF000000);
            graphics.fill(x, xpY, x + xpFilledWidth, xpY + 1, 0xFFFFD700);
        }
    };

    // Допоміжний метод для зміни яскравості кольору
    private static int multiplyColor(int color, float multiplier) {
        int r = (int) (((color >> 16) & 0xFF) * multiplier);
        int g = (int) (((color >> 8) & 0xFF) * multiplier);
        int b = (int) ((color & 0xFF) * multiplier);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}