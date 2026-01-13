package com.maxim.tacionian.energy;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerEnergyProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<PlayerEnergy> PLAYER_ENERGY = CapabilityManager.get(new CapabilityToken<PlayerEnergy>() { });

    private PlayerEnergy energy = null;
    private final LazyOptional<PlayerEnergy> optional = LazyOptional.of(this::createPlayerEnergy);

    private PlayerEnergy createPlayerEnergy() {
        if (this.energy == null) {
            this.energy = new PlayerEnergy();
        }
        return this.energy;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_ENERGY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerEnergy().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerEnergy().loadNBTData(nbt);
    }
}