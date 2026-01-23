package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.api.effects.ITachyonEffect;
import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModItems; // Переконайся, що імпорт є!
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

        // 2. ТЕКСТ (Рівень та Енергія) - ПОВЕРНУТО
        int baseColor = EnergyColorHelper.getColor();

        // Текст зліва (Рівень)
        graphics.drawString(mc.font, Component.translatable("hud.tacionian.level", ClientPlayerEnergy.getLevel()), x, y - 11, 0x00FBFF, true);

        // Текст справа (1000 / 1000 Tx)
        String energyInfo = ClientPlayerEnergy.getEnergy() + " / " + ClientPlayerEnergy.getMaxEnergy() + " Tx";
        graphics.drawString(mc.font, energyInfo, x + barWidth - mc.font.width(energyInfo), y - 11, 0xFFFFFF, true);

        // 3. ОСНОВНИЙ БАР
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + 11, 0xFF353535); // Рамка
        graphics.fill(x, y, x + barWidth, y + 10, 0xFF050505); // Фон

        int fillWidth = (int)(barWidth * Math.min(ClientPlayerEnergy.getRatio(), 1.0f));
        if (fillWidth > 0) {
            graphics.fillGradient(x, y, x + fillWidth, y + 10, baseColor, EnergyColorHelper.darkenColor(baseColor, 0.6f));
        }

        // 4. СМУЖКА ДОСВІДУ (Золота лінія знизу) - ВИПРАВЛЕНО
        int expY = y + 12;
        graphics.fill(x, expY, x + barWidth, expY + 2, 0xFF222222); // Фон смужки досвіду

        float expRatio = ClientPlayerEnergy.getExpRatio();
        int expWidth = (int)(barWidth * Math.min(expRatio, 1.0f));

        if (expWidth > 0) {
            graphics.fill(x, expY, x + expWidth, expY + 2, 0xFFFFAA00);
        }

        // 5. ІКОНКИ
        int iconX = x + barWidth + 6;

        if (ClientPlayerEnergy.isInterfaceStabilized()) {
            renderStatusSlot(graphics, iconX, y - 1, INTERFACE_ICON);
            iconX += 15;
        }
        if (ClientPlayerEnergy.isPlateStabilized()) {
            renderStatusSlot(graphics, iconX, y - 1, PLATE_ICON);
            iconX += 15;
        }
        // Іконка стабілізатора (предмет в руці)
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

    private static void renderStatusSlot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        drawSlotBackground(graphics, x, y);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 2, y + 2, 0);
        graphics.pose().scale(0.5f, 0.5f, 0.5f);
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