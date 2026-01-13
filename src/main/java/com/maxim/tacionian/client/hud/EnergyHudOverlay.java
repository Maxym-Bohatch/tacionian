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

        // Позиція зверху зліва
        int x = 10;
        int y = 10;
        int barWidth = 100;
        int barHeight = 4;

        // Фон підкладки (трохи компактніший)
        graphics.fill(x - 4, y - 4, x + barWidth + 20, y + 42, 0x88000000);

        // Текст рівня (використовуємо переклад)
        String levelText = Component.translatable("tooltip.tacionian.level").getString() + ": " + ClientPlayerEnergy.getLevel();
        graphics.drawString(mc.font, levelText, x, y, 0xFFFFFF, true);

        // Логіка кольору бару
        int color = 0xFF00A0FF; // Синій (за замовчуванням)
        if (ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) {
            color = 0xFF44FF44; // Зелений (стабільно)
        } else if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.getRatio() < 0.1f) {
            color = 0xFFFF4444; // Червоний (небезпека)
        }

        // Рендер бару енергії
        float ratio = ClientPlayerEnergy.getRatio();
        int filledWidth = (int) (barWidth * ratio);

        // Рамка та заповнення енергії
        graphics.fill(x, y + 12, x + barWidth, y + 12 + barHeight, 0xFF222222); // Фон бару
        graphics.fill(x, y + 12, x + filledWidth, y + 12 + barHeight, color); // Прогрес

        // Текст енергії Tx
        String energyText = ClientPlayerEnergy.getEnergy() + " Tx";
        graphics.drawString(mc.font, energyText, x, y + 20, 0xCCCCCC, false);

        // Бар досвіду (дуже тонкий під текстом Tx)
        int xpY = y + 32;
        float expRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();
        graphics.fill(x, xpY, x + barWidth, xpY + 1, 0xFF333333);
        graphics.fill(x, xpY, x + (int)(barWidth * expRatio), xpY + 1, 0xFFFFD700);
    };
}