package com.maxim.tacionian.energy.control;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyControlResolver {

    public static void resolve(ServerPlayer player, PlayerEnergy energy) {
        // 1. Скидаємо статуси на початку кожного тіка
        energy.setStabilized(false);
        energy.setRemoteStabilized(false);
        energy.setRemoteNoDrain(false);

        // 2. Перевірка стабілізатора в інвентарі
        if (hasStabilizerItem(player)) {
            energy.setStabilized(true);
        }

        // 3. Перевірка плити під ногами
        BlockPos pos = player.blockPosition().below();
        BlockState state = player.level().getBlockState(pos);

        if (state.is(ModBlocks.STABILIZATION_PLATE.get())) {
            energy.setRemoteStabilized(true);

            // ЛОГІКА ЗЛИВУ: Якщо гравець присів на плиті
            if (player.isCrouching() && energy.getEnergyPercent() > 5) {
                // Викачуємо 40 Tx за тік
                energy.extractEnergyPure(40, false);

                // Додаємо візуальні ефекти (іскри/дим) навколо ніг
                if (player.tickCount % 4 == 0) {
                    player.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 0.1, player.getZ(),
                            0, 0, 0);
                    energy.sync(player);
                }
            }
        }

        // 4. Глобальна логіка стабілізації (якщо діє предмет або плита)
        if (energy.isStabilized() || energy.isRemoteStabilized()) {

            // Якщо енергія вище 85% — стабілізація "глушить" природну регенерацію ядра
            if (energy.getEnergyPercent() > 85) {
                energy.setRemoteNoDrain(true);
            }

            // М'який запобіжник: тримаємо енергію не вище 95%, щоб не почалася деградація
            if (energy.isOverloaded()) {
                energy.setEnergy((int)(energy.getMaxEnergy() * 0.95f));

                // Синхронізуємо раз на півсекунди, щоб HUD не смикався
                if (player.tickCount % 10 == 0) {
                    energy.sync(player);
                }
            }
        }
    }

    private static boolean hasStabilizerItem(ServerPlayer player) {
        // Перевіряємо весь інвентар на наявність стабілізатора
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModItems.ENERGY_STABILIZER.get())) {
                return true;
            }
        }
        return false;
    }
}