/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.api.effects.ITachyonEffect;
import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class EnergyHudOverlay {
    // Кешуємо іконки
    private static final ItemStack INTERFACE_ICON = new ItemStack(ModBlocks.WIRELESS_ENERGY_INTERFACE.get());
    private static final ItemStack PLATE_ICON = new ItemStack(ModBlocks.STABILIZATION_PLATE.get());
    private static final ItemStack STABILIZER_ICON = new ItemStack(ModItems.ENERGY_STABILIZER.get());

    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();

        // ВИПРАВЛЕННЯ: Тепер HUD не заважає дивитися FPS (F3)
        if (mc.options.renderDebug) return;

        if (mc.level == null || mc.player == null || mc.player.isSpectator()) return;
        if (!ClientPlayerEnergy.hasData()) return;

        int x = TacionianConfig.HUD_X.get();
        int y = TacionianConfig.HUD_Y.get();
        int barWidth = 140;

        graphics.pose().pushPose();

        // 1. Тряска
        float shake = EnergyColorHelper.getShakeAmplitude();
        if (shake > 0) {
            graphics.pose().translate((mc.level.random.nextFloat() - 0.5f) * shake, (mc.level.random.nextFloat() - 0.5f) * shake, 0);
        }

        // 2. ТЕКСТ (Рівень та Енергія)
        int baseColor = EnergyColorHelper.getColor();

        // Текст зліва (Рівень)
        graphics.drawString(mc.font, Component.translatable("hud.tacionian.level", ClientPlayerEnergy.getLevel()), x, y - 11, 0x00FBFF, true);

        // Текст справа (Енергія)
        String energyInfo = ClientPlayerEnergy.getEnergy() + " / " + ClientPlayerEnergy.getMaxEnergy() + " Tx";
        graphics.drawString(mc.font, energyInfo, x + barWidth - mc.font.width(energyInfo), y - 11, 0xFFFFFF, true);

        // 3. ОСНОВНИЙ БАР
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + 11, 0xFF353535); // Рамка
        graphics.fill(x, y, x + barWidth, y + 10, 0xFF050505); // Фон

        int fillWidth = (int)(barWidth * Math.min(ClientPlayerEnergy.getRatio(), 1.0f));
        if (fillWidth > 0) {
            graphics.fillGradient(x, y, x + fillWidth, y + 10, baseColor, EnergyColorHelper.darkenColor(baseColor, 0.6f));
        }

        // 4. СМУЖКА ДОСВІДУ (Золота лінія знизу)
        int expY = y + 12;
        graphics.fill(x, expY, x + barWidth, expY + 2, 0xFF222222);

        float expRatio = ClientPlayerEnergy.getExpRatio();
        int expWidth = (int)(barWidth * Math.min(expRatio, 1.0f));

        if (expWidth > 0) {
            graphics.fill(x, expY, x + expWidth, expY + 2, 0xFFFFAA00);
        }

        // 5. ІКОНКИ СТАТУСУ
        int iconX = x + barWidth + 6;

        if (ClientPlayerEnergy.isInterfaceStabilized()) {
            renderStatusSlot(graphics, iconX, y - 1, INTERFACE_ICON);
            iconX += 15;
        }
        if (ClientPlayerEnergy.isPlateStabilized()) {
            renderStatusSlot(graphics, iconX, y - 1, PLATE_ICON);
            iconX += 15;
        }
        if (ClientPlayerEnergy.isRemoteNoDrain()) {
            renderStatusSlot(graphics, iconX, y - 1, STABILIZER_ICON);
            iconX += 15;
        }

        // Ефекти аддонів
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (effect.getEffect() instanceof ITachyonEffect tachyonEffect) {
                if (tachyonEffect.shouldShowInHud()) {
                    renderStatusSlot(graphics, iconX, y - 1, tachyonEffect.getIcon(effect));
                    iconX += 15;
                }
            }
        }

        graphics.pose().popPose();
    };

    // ПОВЕРНУТО: Масштабування до 0.5x, щоб іконки були акуратними
    private static void renderStatusSlot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        drawSlotBackground(graphics, x, y);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 2, y + 2, 0); // Центрування в слоті 12x12
        graphics.pose().scale(0.5f, 0.5f, 0.5f);    // Той самий розмір
        graphics.renderFakeItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private static void renderStatusSlot(GuiGraphics graphics, int x, int y, ResourceLocation icon) {
        drawSlotBackground(graphics, x, y);
        graphics.blit(icon, x + 1, y + 1, 0, 0, 10, 10, 10, 10);
    }

    private static void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 12, y + 12, 0xAA000000);
        graphics.renderOutline(x, y, 12, 12, 0xFF777777);
    }
}