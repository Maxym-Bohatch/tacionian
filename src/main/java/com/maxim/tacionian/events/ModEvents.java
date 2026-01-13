package com.maxim.tacionian.events;

import com.maxim.tacionian.Tacionian;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
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
                // Копіюємо NBT, щоб зберегти рівень і енергію після смерті
                CompoundTag nbt = new CompoundTag();
                oldStore.saveNBTData(nbt);
                newStore.loadNBTData(nbt);
            });
        });
    }
}