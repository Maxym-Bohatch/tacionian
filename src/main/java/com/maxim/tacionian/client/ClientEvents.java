package com.maxim.tacionian.client;

import com.maxim.tacionian.client.hud.EnergyHudOverlay;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "tacionian", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Реєструємо шар інтерфейсу поверх усіх інших (AboveAll)
        event.registerAboveAll("tacionian_energy", EnergyHudOverlay.HUD);
    }
}