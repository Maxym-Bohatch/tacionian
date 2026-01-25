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

package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyReservoirBlockEntity extends BlockEntity implements ITachyonStorage {
    private int energy = 0;
    private final int MAX_CAPACITY = 25000;
    private final LazyOptional<ITachyonStorage> holder = LazyOptional.of(() -> this);

    public EnergyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESERVOIR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyReservoirBlockEntity be) {
        if (level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(net -> {
                // ПАРИТЕТНА ПЕРЕДАЧА: Вирівнюємо рівень енергії між баком і мережею
                // Якщо в баку відсотково більше енергії, ніж в мережі — віддаємо.
                double myFill = (double)be.energy / be.MAX_CAPACITY;
                double netFill = (double)net.getEnergy() / net.getMaxCapacity();

                if (myFill > netFill) {
                    int toPush = Math.min(be.energy, 500);
                    int accepted = net.receiveTacionEnergy(toPush, false);
                    be.extractTacionEnergy(accepted, false);
                } else if (netFill > myFill) {
                    int toPull = Math.min(net.getEnergy(), 500);
                    int taken = net.extractTacionEnergy(toPull, false);
                    be.receiveTacionEnergy(taken, false);
                }
            });
        }
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        int toAdd = Math.min(amount, MAX_CAPACITY - energy);
        if (!simulate && toAdd > 0) { energy += toAdd; setChanged(); }
        return toAdd;
    }

    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int toTake = Math.min(amount, energy);
        if (!simulate && toTake > 0) { energy -= toTake; setChanged(); }
        return toTake;
    }

    @Override public int getEnergy() { return energy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return holder.cast();
        return super.getCapability(cap, side);
    }

    @Override protected void saveAdditional(CompoundTag nbt) { nbt.putInt("Energy", energy); super.saveAdditional(nbt); }
    @Override public void load(CompoundTag nbt) { super.load(nbt); this.energy = nbt.getInt("Energy"); }
}