/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.api.effects.ITachyonEffect;
import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
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
    private static final ItemStack INTERFACE_ICON = new ItemStack(ModBlocks.WIRELESS_ENERGY_INTERFACE.get());
    private static final ItemStack PLATE_ICON = new ItemStack(ModBlocks.STABILIZATION_PLATE.get());
    private static final ItemStack STABILIZER_ICON = new ItemStack(ModItems.ENERGY_STABILIZER.get());

    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.renderDebug || mc.level == null || mc.player == null || mc.player.isSpectator()) return;
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

        // 2. Рендер основного бару (текст і заповнення)
        int baseColor = EnergyColorHelper.getColor();
        Component levelText = Component.translatable("hud.tacionian.level", ClientPlayerEnergy.getLevel());
        graphics.drawString(mc.font, levelText, x, y - 11, 0x00FBFF, true);

        String energyInfo = ClientPlayerEnergy.getEnergy() + " / " + ClientPlayerEnergy.getMaxEnergy() + " Tx";
        int energyX = Math.max(x + mc.font.width(levelText) + 15, x + barWidth - mc.font.width(energyInfo));
        graphics.drawString(mc.font, energyInfo, energyX, y - 11, 0xFFFFFF, true);

        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + 11, 0xFF353535);
        graphics.fill(x, y, x + barWidth, y + 10, 0xFF050505);
        int fillWidth = (int)(barWidth * Math.min(ClientPlayerEnergy.getRatio(), 1.0f));
        if (fillWidth > 0) graphics.fillGradient(x, y, x + fillWidth, y + 10, baseColor, EnergyColorHelper.darkenColor(baseColor, 0.6f));

        // 3. Іконки статусу
        int iconX = x + barWidth + 6;

        if (ClientPlayerEnergy.isInterfaceStabilized()) {
            renderStatusSlot(graphics, iconX, y - 1, INTERFACE_ICON);
            iconX += 15;
        }
        if (ClientPlayerEnergy.isPlateStabilized()) {
            renderStatusSlot(graphics, iconX, y - 1, PLATE_ICON);
            iconX += 15;
        }

        // --- СПЕЦІАЛЬНИЙ РЕНДЕР СТАБІЛІЗАТОРА ---
        if (ClientPlayerEnergy.isRemoteNoDrain()) {
            ItemStack bestStack = ItemStack.EMPTY;
            int count = 0;
            for (ItemStack item : mc.player.getInventory().items) {
                if (item.getItem() instanceof EnergyStabilizerItem) {
                    count++;
                    if (bestStack.isEmpty() || item.getDamageValue() < bestStack.getDamageValue()) bestStack = item;
                }
            }

            if (!bestStack.isEmpty()) {
                float integrity = 1.0f - ((float)bestStack.getDamageValue() / bestStack.getMaxDamage());

                // Використовуємо пульсуючий фон з EnergyColorHelper
                int bgColor = EnergyColorHelper.getStabilizerAlertColor(integrity);
                drawSlotBackgroundCustom(graphics, iconX, y - 1, bgColor);

                float charge = (float)bestStack.getOrCreateTag().getInt("TachyonBuffer") / 10000f;

                // Ліва (Міцність) та Права (Буфер) смужки
                graphics.fill(iconX - 2, y + 10 - (int)(10 * integrity), iconX - 1, y + 10, 0xFF55FF55);
                graphics.fill(iconX + 13, y + 10 - (int)(10 * charge), iconX + 14, y + 10, 0xFF55FFFF);

                if (count > 1) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);
                    graphics.pose().scale(0.5f, 0.5f, 0.5f);
                    graphics.drawString(mc.font, "x" + count, (iconX + 8) * 2, (y + 9) * 2, 0xFFFFFF, true);
                    graphics.pose().popPose();
                }
                renderStatusSlotIconOnly(graphics, iconX, y - 1, STABILIZER_ICON);
                iconX += 18;
            }
        }

        // Ефекти аддонів
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (effect.getEffect() instanceof ITachyonEffect tachyonEffect && tachyonEffect.shouldShowInHud()) {
                renderStatusSlot(graphics, iconX, y - 1, tachyonEffect.getIcon(effect));
                iconX += 15;
            }
        }
        graphics.pose().popPose();
    };

    private static void renderStatusSlot(GuiGraphics g, int x, int y, ItemStack s) {
        drawSlotBackground(g, x, y);
        renderStatusSlotIconOnly(g, x, y, s);
    }

    private static void renderStatusSlotIconOnly(GuiGraphics g, int x, int y, ItemStack s) {
        g.pose().pushPose();
        g.pose().translate(x + 2, y + 2, 0);
        g.pose().scale(0.5f, 0.5f, 0.5f);
        g.renderFakeItem(s, 0, 0);
        g.pose().popPose();
    }

    // Стандартний фон
    private static void drawSlotBackground(GuiGraphics g, int x, int y) {
        drawSlotBackgroundCustom(g, x, y, 0xAA000000);
    }

    // Кастомний фон (для пульсації тривоги)
    private static void drawSlotBackgroundCustom(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 12, y + 12, color);
        g.renderOutline(x, y, 12, 12, 0xFF777777);
    }

    private static void renderStatusSlot(GuiGraphics g, int x, int y, ResourceLocation icon) {
        drawSlotBackground(g, x, y);
        g.blit(icon, x + 1, y + 1, 0, 0, 10, 10, 10, 10);
    }
}