package com.maxim.tacionian.register;

import com.maxim.tacionian.blocks.StabilizationPlateBlock;
import com.maxim.tacionian.blocks.cable.TachyonCableBlock;
import com.maxim.tacionian.blocks.charger.TachyonChargerBlock;
import com.maxim.tacionian.blocks.wireless.WirelessEnergyInterfaceBlock;
import com.maxim.tacionian.blocks.storage.EnergyReservoirBlock;
import com.maxim.tacionian.items.TacionianBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "tacionian");

    public static final RegistryObject<Block> BASIC_CHARGER_BLOCK = registerBlock("basic_charger_block",
            () -> new TachyonChargerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops(), false));

    public static final RegistryObject<Block> SAFE_CHARGER_BLOCK = registerBlock("safe_charger_block",
            () -> new TachyonChargerBlock(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK).requiresCorrectToolForDrops(), true));

    public static final RegistryObject<Block> STABILIZATION_PLATE = registerBlock("stabilization_plate",
            () -> new StabilizationPlateBlock(BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ENERGY_RESERVOIR = registerBlock("energy_reservoir",
            () -> new EnergyReservoirBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_BLOCK).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> WIRELESS_ENERGY_INTERFACE = registerBlock("wireless_energy_interface",
            () -> new WirelessEnergyInterfaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()));

    // НОВИЙ КАБЕЛЬ: Додано правильну міцність та вимогу інструменту
    public static final RegistryObject<Block> TACHYON_CABLE = registerBlock("tachyon_cable",
            () -> new TachyonCableBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .noOcclusion()
                    .strength(1.5f, 3.0f) // Міцність заліза
                    .requiresCorrectToolForDrops())); // Тепер випадає тільки з правильним інструментом

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new TacionianBlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }
}