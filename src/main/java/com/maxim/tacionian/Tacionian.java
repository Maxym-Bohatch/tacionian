package com.maxim.tacionian;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModItems;
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
        // Отримуємо шину моду (для реєстрації предметів, блоків, капсів)
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Реєстрація контенту
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);

        // 2. Слухачі шини МОДУ
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCaps);

        // 3. Слухачі загальної шини FORGE (Команди та інше)
        // Використовуємо статичне посилання, щоб уникнути помилок з 'this'
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    // Реєстрація команд
    private void onRegisterCommands(RegisterCommandsEvent event) {
        EnergyCommand.register(event.getDispatcher());
    }

    // Реєстрація Capability
    private void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(PlayerEnergy.class);
    }
}