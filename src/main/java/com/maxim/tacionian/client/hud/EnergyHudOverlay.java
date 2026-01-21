package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.api.effects.ITachyonEffect;
import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class EnergyHudOverlay {
    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.isSpectator()) return;
        if (!ClientPlayerEnergy.hasData()) return;

        int x = 15;
        int y = 15;
        int barWidth = 120;
        int barHeight = 8;
        float gameTime = mc.level.getGameTime() + partialTick;

        // ВИКЛИКАЄМО НАШ НОВИЙ ХЕЛПЕР
        int color = EnergyColorHelper.getColor();

        // Додаткова пульсація для тривоги
        if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.isCriticalLow()) {
            float pulse = (float) (Math.sin(gameTime * 0.5f) + 1.0f) * 0.5f * 0.4f + 0.6f;
            color = multiplyColor(color, pulse);
        }

        // Рендер тексту
        graphics.drawString(mc.font, "LVL " + ClientPlayerEnergy.getLevel(), x, y, 0xFFFFFF, true);
        graphics.drawString(mc.font, ClientPlayerEnergy.getEnergy() + " Tx", x + barWidth - 30, y, 0xBBBBBB, true);

        // Бар енергії
        int barY = y + 12;
        float ratio = ClientPlayerEnergy.getRatio();
        graphics.fill(x - 1, barY - 1, x + barWidth + 1, barY + barHeight + 1, 0xFF000000);
        graphics.fill(x, barY, x + barWidth, barY + barHeight, 0xFF151515);
        graphics.fill(x, barY, x + (int)(barWidth * Math.min(ratio, 1.0f)), barY + barHeight, color);

        // Ефект сканування
        if (ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) {
            int scanX = x + (int)((gameTime * 4) % barWidth);
            graphics.fill(scanX, barY, Math.min(scanX + 4, x + barWidth), barY + barHeight, 0x44FFFFFF);
        }

        // --- МАЛЮВАННЯ ІКОНОК ЕФЕКТІВ ---
        int iconX = x;
        int iconY = barY + barHeight + 4;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (effect.getEffect() instanceof ITachyonEffect tachyonEffect && tachyonEffect.shouldShowInHud()) {
                graphics.fill(iconX, iconY, iconX + 6, iconY + 6, 0xFF000000);
                graphics.fill(iconX + 1, iconY + 1, iconX + 5, iconY + 5, tachyonEffect.getIconColor());
                iconX += 8;
            }
        }

        // Рядок досвіду
        int xpY = iconY + 8;
        float xpRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();
        graphics.fill(x, xpY, x + (int)(barWidth * Math.min(xpRatio, 1.0f)), xpY + 1, 0xFFFFD700);
    };

    private static int multiplyColor(int color, float multiplier) {
        int r = (int) (((color >> 16) & 0xFF) * multiplier);
        int g = (int) (((color >> 8) & 0xFF) * multiplier);
        int b = (int) ((color & 0xFF) * multiplier);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}