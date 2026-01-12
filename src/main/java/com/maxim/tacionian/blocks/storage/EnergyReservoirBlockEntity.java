package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyReservoirBlockEntity extends BlockEntity {
    private int storedEnergy = 0;
    private static final int MAX_STORAGE = 100000;

    public EnergyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESERVOIR_BE.get(), pos, state);
    }

    public void fill(int amount) {
        this.storedEnergy = Math.min(storedEnergy + amount, MAX_STORAGE);
        setChanged();
    }

    public int extract(int amount) {
        int toExtract = Math.min(amount, storedEnergy);
        storedEnergy -= toExtract;
        setChanged();
        return toExtract;
    }

    public int getStored() { return storedEnergy; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("stored", storedEnergy);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedEnergy = tag.getInt("stored");
    }
}