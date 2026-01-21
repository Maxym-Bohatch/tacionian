package com.maxim.tacionian.api.effects;

import net.minecraft.world.effect.MobEffect;

public interface ITachyonEffect {
    int getIconColor(); // Колір індикатора в HUD
    boolean shouldShowInHud(); // Чи показувати його взагалі
}