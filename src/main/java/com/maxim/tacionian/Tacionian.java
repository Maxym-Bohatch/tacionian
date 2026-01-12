package com.maxim.tacionian;

import com.maxim.tacionian.command.CommandRegister;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModBlocks;
import com.maxim.tacionian.register.ModCreativeTab;
import com.maxim.tacionian.register.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("tacionian")
public class Tacionian {
    public Tacionian() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Порядок важливий!
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTab.TABS.register(modEventBus);

        // Реєструємо мережу
        NetworkHandler.register();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(CommandRegister::onRegister);
    }
    }
