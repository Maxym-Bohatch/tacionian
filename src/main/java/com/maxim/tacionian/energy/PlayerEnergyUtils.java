package com.maxim.tacionian.energy;

import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class PlayerEnergyUtils {

    /**
     * Перевіряє інвентар гравця і виставляє стани стабілізації
     */
    public static void updateStabilization(ServerPlayer player, PlayerEnergy energy) {

        boolean hasStabilizer = false;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                hasStabilizer = true;
                break;
            }
        }

        energy.setStabilized(hasStabilizer);
    }
}
