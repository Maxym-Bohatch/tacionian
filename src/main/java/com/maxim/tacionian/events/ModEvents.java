package com.maxim.tacionian.events;

import com.maxim.tacionian.Tacionian;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.energy.PlayerEnergyEffects;
import com.maxim.tacionian.energy.control.EnergyControlResolver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Tacionian.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerEnergyProvider.PLAYER_ENERGY).isPresent()) {
                event.addCapability(new ResourceLocation(Tacionian.MOD_ID, "properties"), new PlayerEnergyProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(oldStore -> {
            event.getEntity().getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(newStore -> {
                CompoundTag nbt = new CompoundTag();
                oldStore.saveNBTData(nbt);
                newStore.loadNBTData(nbt);
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.sync(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            ServerPlayer player = (ServerPlayer) event.player;

            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                // Визначаємо стан (плити, предмети)
                EnergyControlResolver.resolve(player, energy);

                // Регенерація та логіка тіка
                energy.tick(player);

                // Шкода та вогонь
                PlayerEnergyEffects.apply(player, energy);

                // СИНХРОНІЗАЦІЯ з HUD (кожні 10 тіків = 0.5 сек)
                if (player.level().getGameTime() % 10 == 0) {
                    energy.sync(player);
                }
            });
        }
    }
}