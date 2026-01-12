package com.maxim.tacionian.register;

import com.maxim.tacionian.items.energy.EnergyCellItem;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import com.maxim.tacionian.items.charger.BasicChargerItem;
import com.maxim.tacionian.items.charger.SafeChargerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "tacionian");

    // Реєстрація звичайних предметів
    public static final RegistryObject<Item> ENERGY_STABILIZER = ITEMS.register("energy_stabilizer",
            () -> new EnergyStabilizerItem(new Item.Properties()));

    public static final RegistryObject<Item> ENERGY_CELL = ITEMS.register("energy_cell",
            () -> new EnergyCellItem(new Item.Properties()));

    public static final RegistryObject<Item> BASIC_CHARGER_ITEM = ITEMS.register("basic_charger_item",
            () -> new BasicChargerItem(new Item.Properties()));

    public static final RegistryObject<Item> SAFE_CHARGER_ITEM = ITEMS.register("safe_charger_item",
            () -> new SafeChargerItem(new Item.Properties()));
}