package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.api.effects.ITachyonEffect;
import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class EnergyHudOverlay {
    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.isSpectator()) return;
        if (!ClientPlayerEnergy.hasData()) return;

        int x = 15;
        int y = 15;
        int barWidth = 140;
        int barHeight = 8;
        float gameTime = mc.level.getGameTime() + partialTick;

        int color = EnergyColorHelper.getColor();

        if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.isCriticalLow()) {
            float pulse = (float) (Math.sin(gameTime * 0.5f) + 1.0f) * 0.5f * 0.4f + 0.6f;
            color = multiplyColor(color, pulse);
        }

        // --- ЛОКАЛІЗОВАНИЙ ТЕКСТ ---
        // Використовуємо ключі локалізації замість прямого тексту
        String lvlText = Component.translatable("hud.tacionian.level", ClientPlayerEnergy.getLevel()).getString();
        graphics.drawString(mc.font, lvlText, x, y, 0xFFFFFF, true);

        // Форматуємо рядок енергії: "Енергія / Максимум Одиниця"
        String energyUnit = Component.translatable("hud.tacionian.energy_unit").getString();
        String energyInfo = String.format("%d / %d %s",
                ClientPlayerEnergy.getEnergy(),
                ClientPlayerEnergy.getMaxEnergy(),
                energyUnit);

        int infoWidth = mc.font.width(energyInfo);
        graphics.drawString(mc.font, energyInfo, x + barWidth - infoWidth, y, 0xBBBBBB, true);

        // --- БАР ЕНЕРГІЇ ---
        int barY = y + 12;
        float ratio = ClientPlayerEnergy.getRatio();

        graphics.fill(x - 1, barY - 1, x + barWidth + 1, barY + barHeight + 1, 0xFF000000);
        graphics.fill(x, barY, x + barWidth, barY + barHeight, 0xFF151515);

        int fillWidth = (int)(barWidth * Math.min(ratio, 1.0f));
        graphics.fill(x, barY, x + fillWidth, barY + barHeight, color);

        if (ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) {
            int scanX = x + (int)((gameTime * 4) % barWidth);
            graphics.fill(scanX, barY, Math.min(scanX + 4, x + barWidth), barY + barHeight, 0x44FFFFFF);
        }

        // --- ЕФЕКТИ ТА ДОСВІД ---
        int iconX = x;
        int iconY = barY + barHeight + 4;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (effect.getEffect() instanceof ITachyonEffect tachyonEffect && tachyonEffect.shouldShowInHud()) {
                graphics.fill(iconX, iconY, iconX + 8, iconY + 8, 0xFF000000);
                graphics.fill(iconX + 1, iconY + 1, iconX + 7, iconY + 7, tachyonEffect.getIconColor());
                iconX += 10;
            }
        }

        int xpY = iconY + 10;
        float xpRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();
        graphics.fill(x, xpY, x + barWidth, xpY + 1, 0xFF333333);
        graphics.fill(x, xpY, x + (int)(barWidth * Math.min(xpRatio, 1.0f)), xpY + 1, 0xFFFFD700);
    };

    private static int multiplyColor(int color, float multiplier) {
        int r = (int) (((color >> 16) & 0xFF) * multiplier);
        int g = (int) (((color >> 8) & 0xFF) * multiplier);
        int b = (int) ((color & 0xFF) * multiplier);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}