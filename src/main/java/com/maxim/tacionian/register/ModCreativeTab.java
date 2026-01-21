package com.maxim.tacionian.register;

import com.maxim.tacionian.Tacionian;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tacionian.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TACIONIAN_TAB = TABS.register("tacionian_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tacionian_tab"))
                    .icon(() -> new ItemStack(ModItems.ENERGY_STABILIZER.get())) // Переконайся, що цей предмет існує в ModItems
                    .displayItems((params, output) -> {
                        output.accept(new ItemStack(ModItems.ENERGY_STABILIZER.get()));
                        output.accept(new ItemStack(ModItems.ENERGY_CELL.get()));
                        output.accept(new ItemStack(ModItems.BASIC_CHARGER_ITEM.get())); // Твій предмет
                        output.accept(new ItemStack(ModItems.SAFE_CHARGER_ITEM.get()));

                        output.accept(new ItemStack(ModBlocks.BASIC_CHARGER_BLOCK.get())); // Твій блок (тепер з ітемом!)
                        output.accept(new ItemStack(ModBlocks.SAFE_CHARGER_BLOCK.get()));
                        output.accept(new ItemStack(ModBlocks.STABILIZATION_PLATE.get()));
                        output.accept(new ItemStack(ModBlocks.ENERGY_RESERVOIR.get()));
                        output.accept(new ItemStack(ModBlocks.WIRELESS_ENERGY_INTERFACE.get()));
                        output.accept(new ItemStack(ModBlocks.TACHYON_CABLE.get()));
                    })
                    .build());

    // Цей метод ми викликаємо в головному класі
    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}