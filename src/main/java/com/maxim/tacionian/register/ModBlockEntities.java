package com.maxim.tacionian.register;

import com.maxim.tacionian.Tacionian; // Додай імпорт свого головного класу
import com.maxim.tacionian.blocks.charger.TachyonChargerBlockEntity;
import com.maxim.tacionian.blocks.storage.EnergyReservoirBlockEntity;
import com.maxim.tacionian.blocks.wireless.WirelessEnergyInterfaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    // Краще використовувати Tacionian.MOD_ID замість ручного тексту "tacionian"
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "tacionian");

    public static final RegistryObject<BlockEntityType<WirelessEnergyInterfaceBlockEntity>> WIRELESS_BE =
            BLOCK_ENTITIES.register("wireless_be", () -> BlockEntityType.Builder.of(
                    WirelessEnergyInterfaceBlockEntity::new,
                    ModBlocks.WIRELESS_ENERGY_INTERFACE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<TachyonChargerBlockEntity>> CHARGER_BE =
            BLOCK_ENTITIES.register("charger_be", () -> BlockEntityType.Builder.of(
                    TachyonChargerBlockEntity::new,
                    ModBlocks.BASIC_CHARGER_BLOCK.get(),
                    ModBlocks.SAFE_CHARGER_BLOCK.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<EnergyReservoirBlockEntity>> RESERVOIR_BE =
            BLOCK_ENTITIES.register("reservoir_be", () -> BlockEntityType.Builder.of(
                    EnergyReservoirBlockEntity::new,
                    ModBlocks.ENERGY_RESERVOIR.get()
            ).build(null));

    // МЕТОД, ЯКИЙ МАЄ БУТИ ВИКЛИКАНИЙ У ГОЛОВНОМУ КЛАСІ
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}