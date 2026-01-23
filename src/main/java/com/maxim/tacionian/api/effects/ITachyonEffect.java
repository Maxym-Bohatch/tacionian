package com.maxim.tacionian.api.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

public interface ITachyonEffect {
    int getIconColor(); // Колір індикатора в HUD
    boolean shouldShowInHud(); // Чи показувати його взагалі
    ResourceLocation getIcon(MobEffectInstance instance);
    default int getEffectColor() { return 0xFFFFFF; }
}