package com.maxim.tacionian.energy;

import net.minecraft.world.item.ItemStack;

public interface ITachyonChargeable {
    int getMaxEnergy(ItemStack stack);
    int getEnergy(ItemStack stack);
    int receiveEnergy(ItemStack stack, int amount, boolean simulate);
    int extractEnergy(ItemStack stack, int amount, boolean simulate);

    default boolean isFull(ItemStack stack) {
        return getEnergy(stack) >= getMaxEnergy(stack);
    }
}