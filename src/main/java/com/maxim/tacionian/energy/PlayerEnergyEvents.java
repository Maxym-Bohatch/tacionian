package com.maxim.tacionian.energy;

import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.register.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = "tacionian")
public class PlayerEnergyEvents {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.player instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                if (event.phase == TickEvent.Phase.START) {
                    energy.tick(player);

                    // ПАСИВНИЙ ЗАХИСТ ІТЕМУ
                    boolean hasStabilizer = player.getInventory().contains(new ItemStack(ModItems.ENERGY_STABILIZER.get()));
                    if (hasStabilizer) {
                        energy.setStabilized(true);
                        if (energy.isOverloaded()) {
                            energy.setEnergy((int)(energy.getMaxEnergy() * 0.95f)); // Без досвіду
                        }
                    }
                } else {
                    PlayerEnergyEffects.apply(player, energy);
                    if (player.tickCount % 2 == 0) {
                        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(energy));
                    }
                }
            });
        }
    }
}