package com.maxim.tacionian;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModItems;
import com.maxim.tacionian.command.EnergyCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Tacionian.MOD_ID)
public class Tacionian {
    public static final String MOD_ID = "tacionian";

    public Tacionian() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCaps); // Реєструємо капси правильно через лістенер

        // РЕЄСТРАЦІЯ КОМАНД (на правильній шині!)
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Решта ігрових подій (якщо є методи з @SubscribeEvent для Forge Bus)
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    // Подія для команд (БЕЗ @SubscribeEvent, бо ми додали addListener в конструкторі)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EnergyCommand.register(event.getDispatcher());
    }

    // Подія для Капабіліті (через Mod Bus)
    public void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(PlayerEnergy.class);
    }
}