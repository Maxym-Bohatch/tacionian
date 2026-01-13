package com.maxim.tacionian.energy.control;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModItems; // Переконайся, що ітем там
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyControlResolver {

    public static void resolve(ServerPlayer player, PlayerEnergy energy) {
        // Скидаємо статуси кожного тіка, щоб вони оновилися лише якщо умова виконується
        energy.setStabilized(false);
        energy.setRemoteStabilized(false);
        energy.setRemoteNoDrain(false);

        // 1. Перевірка предмета в інвентарі (Стабілізатор)
        if (hasStabilizerItem(player)) {
            energy.setStabilized(true);
        }

        // 2. Перевірка блоку ПІД ногами (Плита)
        BlockPos pos = player.blockPosition().below();
        BlockState state = player.level().getBlockState(pos);

        if (state.is(ModBlocks.STABILIZATION_PLATE.get())) {
            energy.setRemoteStabilized(true);
        }

        // 3. Логіка дії стабілізації
        if (energy.isStabilized() || energy.isRemoteStabilized()) {
            if (energy.isOverloaded()) {
                // М'яко тримаємо енергію на межі, не даючи вибухнути
                energy.setEnergy((int)(energy.getMaxEnergy() * 0.95f));
            }
        }
    }

    private static boolean hasStabilizerItem(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            // Переконайся, що клас ітема або RegistryObject вказано вірно
            if (stack.is(ModItems.ENERGY_STABILIZER.get())) return true;
        }
        return false;
    }
}