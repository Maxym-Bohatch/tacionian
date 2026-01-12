package com.maxim.tacionian.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "tacionian");

    public static final RegistryObject<CreativeModeTab> TACIONIAN_TAB = TABS.register("tacionian_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tacionian_tab"))
            .icon(() -> new ItemStack(ModItems.ENERGY_STABILIZER.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.ENERGY_STABILIZER.get());
                output.accept(ModItems.ENERGY_CELL.get());
                output.accept(ModItems.BASIC_CHARGER_ITEM.get());
                output.accept(ModItems.SAFE_CHARGER_ITEM.get());
                output.accept(ModBlocks.STABILIZATION_PLATE.get());
                output.accept(ModBlocks.BASIC_CHARGER_BLOCK.get());
                output.accept(ModBlocks.SAFE_CHARGER_BLOCK.get());
                output.accept(ModBlocks.RESERVOIR_BLOCK.get());
                output.accept(ModBlocks.WIRELESS_INTERFACE.get());
            })
            .build());
}