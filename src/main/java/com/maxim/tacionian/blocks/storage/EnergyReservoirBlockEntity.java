package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyReservoirBlockEntity extends BlockEntity {
    private int energy = 0;
    private final int MAX_CAPACITY = 100000;

    public EnergyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESERVOIR_BE.get(), pos, state);
    }

    public int receiveTacionEnergy(int amount, boolean simulate) {
        int space = MAX_CAPACITY - energy;
        int toAdd = Math.min(amount, space);
        if (!simulate && toAdd > 0) {
            energy += toAdd;
            setChanged();
        }
        return toAdd;
    }

    public int extractTacionEnergy(int amount, boolean simulate) {
        int toExtract = Math.min(amount, energy);
        if (!simulate && toExtract > 0) {
            energy -= toExtract;
            setChanged();
        }
        return toExtract;
    }

    public int getEnergy() { return energy; }
    public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.energy = nbt.getInt("StoredTacion");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("StoredTacion", energy);
    }

    // Потрібно для того, щоб при ламанні блоку дані могли зберегтися в предмет (опціонально)
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}