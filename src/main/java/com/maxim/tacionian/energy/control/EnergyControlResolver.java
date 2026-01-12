package com.maxim.tacionian.energy.control;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnergyControlResolver {

    public static void resolve(ServerPlayer player, PlayerEnergy energy) {
        // Скидаємо статуси (вони мають оновитися блоками/предметами в цьому ж тику)
        energy.setStabilized(false);
        energy.setRemoteStabilized(false);
        // НЕ скидаємо remoteNoDrain тут, якщо він має триматися довше,
        // АБО переконайся, що блок Interface викликає setRemoteNoDrain(true) кожного тику!
        energy.setRemoteNoDrain(false);

        if (hasStabilizerItem(player)) {
            energy.setStabilized(true);
        }

        // Якщо працює стабілізація
        if (energy.isStabilized() || energy.isRemoteStabilized()) {
            if (energy.isOverloaded()) {
                energy.setEnergy((int)(energy.getMaxEnergy() * 0.96f));
            }
        }
    }

    private static boolean hasStabilizerItem(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof EnergyStabilizerItem) return true;
        }
        return false;
    }
}