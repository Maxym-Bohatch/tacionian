package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class EnergyHudOverlay {
    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.isSpectator()) return;
        if (!ClientPlayerEnergy.hasData()) return;

        int x = 10;
        int y = 10;
        int barWidth = 100;
        int barHeight = 6;

        float ratio = ClientPlayerEnergy.getRatio();
        int filled = (int) (barWidth * Math.max(0, Math.min(1, ratio)));

        // Фон
        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + 36, 0x88000000);
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);

        // Колір бару
        int barColor = 0xFF00A0FF; // Default Blue
        boolean isStable = ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized();

        if (isStable) barColor = 0xFF00FF00; // Green
        else if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.isCriticalLow()) barColor = 0xFFFF0000; // Red

        graphics.fill(x, y, x + filled, y + barHeight, barColor);

        // Текст
        graphics.drawString(mc.font, "Level: " + ClientPlayerEnergy.getLevel(), x, y + 10, 0xFFFFFFFF, true);
        graphics.drawString(mc.font, ClientPlayerEnergy.getEnergy() + " Tx", x, y + 20, 0xCCCCCC, true);

        // Експа
        float expRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();
        graphics.fill(x, y + 30, x + barWidth, y + 32, 0xFF222200);
        graphics.fill(x, y + 30, x + (int)(barWidth * expRatio), y + 32, 0xFFFFFF00);
    };
}