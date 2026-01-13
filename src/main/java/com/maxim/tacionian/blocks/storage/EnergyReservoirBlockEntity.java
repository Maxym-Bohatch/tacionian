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
        if (!simulate) { energy += toAdd; setChanged(); }
        return toAdd;
    }

    public int extractTacionEnergy(int amount, boolean simulate) {
        int toExtract = Math.min(amount, energy);
        if (!simulate) { energy -= toExtract; setChanged(); }
        return toExtract;
    }

    @Override
    public void load(CompoundTag nbt) { super.load(nbt); this.energy = nbt.getInt("StoredTacion"); }
    @Override
    protected void saveAdditional(CompoundTag nbt) { super.saveAdditional(nbt); nbt.putInt("StoredTacion", energy); }
}