package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.api.effects.ITachyonEffect;
import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.mojang.blaze3d.systems.RenderSystem;
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
        int barHeight = 10; // Трохи вищий бар для солідності
        float gameTime = mc.level.getGameTime() + partialTick;

        // Отримуємо базовий колір
        int baseColor = EnergyColorHelper.getColor();

        // --- 1. ТЕКСТ (З тінню для читабельності) ---
        String lvlText = Component.translatable("hud.tacionian.level", ClientPlayerEnergy.getLevel()).getString();
        graphics.drawString(mc.font, lvlText, x, y - 10, 0xFFFFFF, true); // Підняв текст над баром

        String energyInfo = String.format("%d / %d Tx", ClientPlayerEnergy.getEnergy(), ClientPlayerEnergy.getMaxEnergy());
        int infoWidth = mc.font.width(energyInfo);
        graphics.drawString(mc.font, energyInfo, x + barWidth - infoWidth, y - 10, 0xAAAAAA, true);

        // --- 2. РАМКА (Металевий ефект) ---
        // Зовнішня темна обводка
        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xFF101010);
        // Внутрішня світла рамка (відблиск металу)
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF353535);
        // Фон самого бару (темний, майже чорний)
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF050505);

        // --- 3. ЗАПОВНЕННЯ (Градієнт) ---
        float ratio = ClientPlayerEnergy.getRatio();
        int fillWidth = (int)(barWidth * Math.min(ratio, 1.0f));

        if (fillWidth > 0) {
            // Створюємо темнішу версію кольору для нижньої частини градієнта
            int colorTop = baseColor;
            int colorBottom = darkenColor(baseColor, 0.7f); // Темніший низ

            // Малюємо градієнт (зверху вниз)
            graphics.fillGradient(x, y, x + fillWidth, y + barHeight, colorTop, colorBottom);

            // Додаємо "відблиск скла" зверху (напівпрозорий білий)
            graphics.fill(x, y, x + fillWidth, y + 2, 0x33FFFFFF);
        }

        // --- 4. СКАНЕР (Тільки по заповненій частині, м'який) ---
        if ((ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) && fillWidth > 5) {
            int scanPos = (int)((gameTime * 2.5) % fillWidth);
            // Основна смужка
            graphics.fill(x + scanPos, y, Math.min(x + scanPos + 2, x + fillWidth), y + barHeight, 0x55FFFFFF);
            // Розмиття по боках (фейковий блум)
            if (scanPos > 0) graphics.fill(x + scanPos - 1, y, x + scanPos, y + barHeight, 0x22FFFFFF);
            if (scanPos + 3 < fillWidth) graphics.fill(x + scanPos + 2, y, x + scanPos + 3, y + barHeight, 0x22FFFFFF);
        }

        // --- 5. XP BAR (Інтегрований в нижню рамку) ---
        int xpY = y + barHeight + 3;
        float xpRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();

        // Підкладка XP
        graphics.fill(x, xpY, x + barWidth, xpY + 2, 0xFF101010);
        // Сама смужка (Золотий градієнт)
        graphics.fillGradient(x, xpY, x + (int)(barWidth * Math.min(xpRatio, 1.0f)), xpY + 2, 0xFFFFD700, 0xFFFFAA00);

        // --- 6. ЕФЕКТИ ---
        int iconX = x;
        int iconY = xpY + 6;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (effect.getEffect() instanceof ITachyonEffect tachyonEffect && tachyonEffect.shouldShowInHud()) {
                // Тінь іконки
                graphics.fill(iconX, iconY, iconX + 8, iconY + 8, 0xFF000000);
                // Сама іконка
                graphics.fill(iconX + 1, iconY + 1, iconX + 7, iconY + 7, tachyonEffect.getIconColor());
                iconX += 10;
            }
        }
    };

    private static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}