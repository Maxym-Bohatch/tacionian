package com.maxim.tacionian.register;

import com.maxim.tacionian.blocks.StabilizationPlateBlock;
import com.maxim.tacionian.blocks.charger.TachyonChargerBlock;
import com.maxim.tacionian.blocks.storage.EnergyReservoirBlock;
import com.maxim.tacionian.blocks.wireless.WirelessEnergyInterfaceBlock;
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

    // Зарядники
    public static final RegistryObject<Block> BASIC_CHARGER_BLOCK = registerBlock("basic_charger_block",
            () -> new TachyonChargerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK), false));

    public static final RegistryObject<Block> SAFE_CHARGER_BLOCK = registerBlock("safe_charger_block",
            () -> new TachyonChargerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK), true));

    // Плита стабілізації (ВИПРАВЛЕНО: додано noOcclusion та dynamicShape)
    public static final RegistryObject<Block> STABILIZATION_PLATE = registerBlock("stabilization_plate",
            () -> new StabilizationPlateBlock(BlockBehaviour.Properties.copy(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
                    .noOcclusion()
                    .dynamicShape()));

    // Бездротовий інтерфейс
    public static final RegistryObject<Block> WIRELESS_INTERFACE = registerBlock("wireless_energy_interface",
            () -> new WirelessEnergyInterfaceBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).noOcclusion()));

    // Резервуар
    public static final RegistryObject<Block> RESERVOIR_BLOCK = registerBlock("energy_reservoir",
            () -> new EnergyReservoirBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new TacionianBlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }
}