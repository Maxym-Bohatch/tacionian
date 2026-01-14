package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.mojang.blaze3d.systems.RenderSystem;
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

        // Позиція (нижній лівий кут, над здоров'ям/експою або збоку)
        int x = 10;
        int y = height - 50;

        int barWidth = 100;
        int barHeight = 6;

        // 1. Ефект "дихання" для критичних станів
        float gameTime = mc.level.getGameTime() + partialTick;
        float flash = 0;
        if (ClientPlayerEnergy.isOverloaded() || ClientPlayerEnergy.isCriticalLow()) {
            flash = (float) Math.abs(Math.sin(gameTime * 0.2f)) * 0.4f;
        }

        // 2. Малюємо стильну підкладку (Blur/Shadow effect)
        graphics.fill(x - 2, y - 12, x + barWidth + 22, y + 25, 0x99000000); // Основна плашка
        graphics.fill(x - 2, y - 13, x + barWidth + 22, y - 12, 0xCC00A0FF); // Верхня тонка лінія (стиль)

        // 3. Рендер іконки та тексту рівня
        String levelStr = "LVL " + ClientPlayerEnergy.getLevel();
        graphics.drawString(mc.font, levelStr, x, y - 10, 0xFFFFAA00, true);

        // 4. Логіка кольору (Градієнт або солід)
        int color;
        if (ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) {
            color = 0xFF44FF44; // Стабільний зелений
        } else if (ClientPlayerEnergy.isOverloaded()) {
            color = 0xFFFF4444; // Критичне перевантаження
        } else if (ClientPlayerEnergy.getRatio() < 0.2f) {
            color = 0xFFFFAA00; // Попередження (оранжевий)
        } else {
            color = 0xFF00A0FF; // Стандартний блакитний
        }

        // Додаємо флеш-ефект до кольору
        if (flash > 0) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            color = 0xFF << 24 | (int)(r + (255-r)*flash) << 16 | (int)(g + (255-g)*flash) << 8 | (int)(b + (255-b)*flash);
        }

        // 5. Фон бару (порожній)
        graphics.fill(x, y + 2, x + barWidth, y + 2 + barHeight, 0xFF222222);

        // 6. Заповнення бару з невеликим градієнтом (через два прямокутника)
        float ratio = ClientPlayerEnergy.getRatio();
        int filledWidth = (int) (barWidth * Math.min(ratio, 1.0f));
        graphics.fill(x, y + 2, x + filledWidth, y + 2 + barHeight, color);
        graphics.fill(x, y + 2, x + filledWidth, y + 3, 0x44FFFFFF); // Відблиск зверху бару

        // 7. Текст енергії Tx (компактно справа)
        String energyValue = ClientPlayerEnergy.getEnergy() + " Tx";
        int textWidth = mc.font.width(energyValue);
        graphics.drawString(mc.font, energyValue, x + barWidth - textWidth, y - 10, 0xFFFFFFFF, true);

        // 8. Смужка досвіду (XP) - дуже тонка знизу
        float expRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();
        graphics.fill(x, y + 10, x + barWidth, y + 11, 0xFF333333); // Фон XP
        graphics.fill(x, y + 10, x + (int)(barWidth * expRatio), y + 11, 0xFFFFD700); // Золотий досвід

        // 9. Статус-індикатори (Текст знизу)
        if (ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) {
            graphics.pose().pushPose();
            graphics.pose().scale(0.7f, 0.7f, 0.7f);
            graphics.drawString(mc.font, "◆ STABLE", (int)((x)/0.7f), (int)((y + 14)/0.7f), 0xFF44FF44, false);
            graphics.pose().popPose();
        }
    };
}