package com.maxim.tacionian;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModItems;
import com.maxim.tacionian.register.ModCreativeTab; // Переконайся, що імпорт правильний
import com.maxim.tacionian.command.EnergyCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Tacionian.MOD_ID)
public class Tacionian {
    public static final String MOD_ID = "tacionian";

    public Tacionian() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Реєструємо блоки ТА їхні Block Entities (ОБОВ'ЯЗКОВО)
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.register(modBus); // ДОДАНО ЦЕЙ РЯДОК

        // 2. Реєструємо предмети та вкладку
        ModItems.ITEMS.register(modBus);
        ModCreativeTab.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCaps);

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        EnergyCommand.register(event.getDispatcher());
    }

    private void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(PlayerEnergy.class);
    }
}