package com.maxim.tacionian.register;

import com.maxim.tacionian.blocks.StabilizationPlateBlockEntity; // Перевір шлях до пакету плити
import com.maxim.tacionian.blocks.cable.TachyonCableBlockEntity;
import com.maxim.tacionian.blocks.charger.TachyonChargerBlockEntity;
import com.maxim.tacionian.blocks.storage.EnergyReservoirBlockEntity;
import com.maxim.tacionian.blocks.wireless.WirelessEnergyInterfaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "tacionian");

    // БЕЗДРОТОВИЙ ІНТЕРФЕЙС
    public static final RegistryObject<BlockEntityType<WirelessEnergyInterfaceBlockEntity>> WIRELESS_BE =
            BLOCK_ENTITIES.register("wireless_be", () -> BlockEntityType.Builder.of(
                    WirelessEnergyInterfaceBlockEntity::new, ModBlocks.WIRELESS_ENERGY_INTERFACE.get()).build(null));

    // ЗАРЯДНИКИ (Базовий та Безпечний)
    public static final RegistryObject<BlockEntityType<TachyonChargerBlockEntity>> CHARGER_BE =
            BLOCK_ENTITIES.register("charger_be", () -> BlockEntityType.Builder.of(
                    TachyonChargerBlockEntity::new, ModBlocks.BASIC_CHARGER_BLOCK.get(), ModBlocks.SAFE_CHARGER_BLOCK.get()).build(null));

    // РЕЗЕРВУАР
    public static final RegistryObject<BlockEntityType<EnergyReservoirBlockEntity>> RESERVOIR_BE =
            BLOCK_ENTITIES.register("reservoir_be", () -> BlockEntityType.Builder.of(
                    EnergyReservoirBlockEntity::new, ModBlocks.ENERGY_RESERVOIR.get()).build(null));

    // КАБЕЛЬ
    public static final RegistryObject<BlockEntityType<TachyonCableBlockEntity>> CABLE_BE =
            BLOCK_ENTITIES.register("cable_be", () -> BlockEntityType.Builder.of(
                    TachyonCableBlockEntity::new, ModBlocks.TACHYON_CABLE.get()).build(null));

    // СТАБІЛІЗУЮЧА ПЛИТА
    public static final RegistryObject<BlockEntityType<StabilizationPlateBlockEntity>> STABILIZER_PLATE_BE =
            BLOCK_ENTITIES.register("stabilizer_plate_be", () -> BlockEntityType.Builder.of(
                    StabilizationPlateBlockEntity::new, ModBlocks.STABILIZATION_PLATE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}