package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class EnergyHudOverlay {
    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.isSpectator()) return;
        if (!ClientPlayerEnergy.hasData()) return;

        int x = 15;
        int y = height / 2 - 40;
        int barWidth = 90;
        int barHeight = 6;

        // Малюємо фон підкладки
        graphics.fill(x - 5, y - 15, x + barWidth + 5, y + 45, 0x99000000);

        // Текст рівня
        graphics.drawString(mc.font, "Core Lvl: " + ClientPlayerEnergy.getLevel(), x, y - 10, 0xFFFFFF, true);

        // Логіка кольору бару
        int color = 0xFF00A0FF; // Синій за замовчуванням
        if (ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) {
            color = 0xFF00FF00; // Зелений (стабільно)
        } else if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.isCriticalLow()) {
            color = 0xFFFF0000; // Червоний (небезпека)
        }

        // Рендер бару енергії
        float ratio = ClientPlayerEnergy.getRatio();
        int filledWidth = (int) (barWidth * ratio);

        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000); // Рамка
        graphics.fill(x, y, x + filledWidth, y + barHeight, color); // Заповнення

        // Текст Tx
        graphics.drawString(mc.font, ClientPlayerEnergy.getEnergy() + " / " + ClientPlayerEnergy.getMaxEnergy() + " Tx", x, y + 10, 0xAAAAAA, false);

        // Бар досвіду (маленький під низом)
        int xpY = y + 25;
        float expRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();
        graphics.fill(x, xpY, x + barWidth, xpY + 2, 0xFF222222);
        graphics.fill(x, xpY, x + (int)(barWidth * expRatio), xpY + 2, 0xFFFFFF00);
    };
}