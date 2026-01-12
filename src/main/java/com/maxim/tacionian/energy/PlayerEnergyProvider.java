package com.maxim.tacionian.energy;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerEnergyProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerEnergy> PLAYER_ENERGY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerEnergy energy = new PlayerEnergy();
    private final LazyOptional<PlayerEnergy> optional = LazyOptional.of(() -> energy);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_ENERGY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        // Викликаємо твій метод збереження з класу PlayerEnergy
        energy.saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // Викликаємо твій метод завантаження з класу PlayerEnergy
        energy.loadNBTData(nbt);
    }
}