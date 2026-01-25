/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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