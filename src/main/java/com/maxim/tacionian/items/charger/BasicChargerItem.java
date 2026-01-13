package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class BasicChargerItem extends Item {
    public BasicChargerItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Перевіряємо, що ми на сервері і що сутність - це гравець
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) return;

        serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            if (pEnergy.getEnergy() <= 0) return;

            boolean changed = false;
            for (ItemStack target : serverPlayer.getInventory().items) {
                // Не заряджаємо порожні слоти або самого себе
                if (target.isEmpty() || target == stack) continue;

                // Перевіряємо, чи підтримує предмет RF/FE енергію
                if (target.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                    target.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
                        int neededRF = cap.receiveEnergy(500, true); // Пробуємо передати 500 RF
                        if (neededRF > 0) {
                            int txToTake = neededRF / 10; // Курс 1 до 10

                            // Тепер передаємо serverPlayer - помилка зникне
                            int takenTx = pEnergy.extractEnergyWithExp(txToTake, false, serverPlayer);

                            cap.receiveEnergy(takenTx * 10, false);
                        }
                    });
                    changed = true;
                }
            }

            // Якщо ми витратили енергію, оновлюємо HUD раз на кілька тіків (або кожен раз)
            if (changed && level.getGameTime() % 5 == 0) {
                pEnergy.sync(serverPlayer);
            }
        });
    }
}