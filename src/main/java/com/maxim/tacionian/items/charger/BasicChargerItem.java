package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class BasicChargerItem extends Item {
    public BasicChargerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            if (pEnergy.getEnergy() <= 0) return;

            for (ItemStack target : player.getInventory().items) {
                if (target.isEmpty() || target == stack) continue;
                target.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
                    int neededRF = cap.receiveEnergy(500, true);
                    if (neededRF > 0) {
                        int txToTake = neededRF / 10; // Курс 1:10
                        int takenTx = pEnergy.extractEnergyWithExp(txToTake, false, player);
                        cap.receiveEnergy(takenTx * 10, false);
                    }
                });
            }
        });
    }
}