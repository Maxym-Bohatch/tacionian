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
        int y = 20;
        int barWidth = 140;
        int barHeight = 10;
        float gameTime = mc.level.getGameTime() + partialTick;

        graphics.pose().pushPose();

        float shake = EnergyColorHelper.getShakeAmplitude();
        if (shake > 0) {
            graphics.pose().translate((mc.level.random.nextFloat() - 0.5f) * shake, (mc.level.random.nextFloat() - 0.5f) * shake, 0);
        }

        int baseColor = EnergyColorHelper.getColor();

        graphics.drawString(mc.font, "LVL " + ClientPlayerEnergy.getLevel(), x, y - 11, 0x00FBFF, true);
        String energyInfo = ClientPlayerEnergy.getEnergy() + " / " + ClientPlayerEnergy.getMaxEnergy() + " Tx";
        graphics.drawString(mc.font, energyInfo, x + barWidth - mc.font.width(energyInfo), y - 11, 0xFFFFFF, true);

        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF353535);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF050505);

        float ratio = Math.min(ClientPlayerEnergy.getRatio(), 1.0f);
        int fillWidth = (int)(barWidth * ratio);
        if (fillWidth > 0) {
            graphics.fillGradient(x, y, x + fillWidth, y + barHeight, baseColor, darkenColor(baseColor, 0.6f));
            graphics.fill(x, y, x + fillWidth, y + 1, 0x44FFFFFF);
        }

        if ((ClientPlayerEnergy.isStabilized() || ClientPlayerEnergy.isRemoteStabilized()) && fillWidth > 5) {
            int scanPos = (int)((gameTime * 3.0) % barWidth);
            if (scanPos < fillWidth) {
                graphics.fill(x + scanPos, y, Math.min(x + scanPos + 10, x + fillWidth), y + barHeight, 0x33FFFFFF);
            }
        }

        // ШКАЛА ДОСВІДУ - 1 ПІКСЕЛЬ
        int xpY = y + barHeight + 3;
        int xpHeight = 1;
        float xpRatio = (float) ClientPlayerEnergy.getExperience() / ClientPlayerEnergy.getRequiredExp();
        graphics.fill(x, xpY, x + barWidth, xpY + xpHeight, 0x44000000);
        int xpFillWidth = (int)(barWidth * Math.min(xpRatio, 1.0f));
        if (xpFillWidth > 0) {
            graphics.fill(x, xpY, x + xpFillWidth, xpY + xpHeight, 0xFFFFD700);
        }

        int iconX = x;
        int iconY = xpY + xpHeight + 4;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (effect.getEffect() instanceof ITachyonEffect tachyonEffect && tachyonEffect.shouldShowInHud()) {
                graphics.fill(iconX, iconY, iconX + 6, iconY + 6, tachyonEffect.getIconColor());
                iconX += 8;
            }
        }

        graphics.pose().popPose();
    };

    private static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}