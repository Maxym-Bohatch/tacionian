package com.maxim.tacionian.blocks.cable;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TachyonCableBlockEntity extends BlockEntity implements ITachyonStorage {
    private int energy = 0;
    private final int MAX_CAPACITY = 1000;
    private final int MAX_TRANSFER = 200;

    private final LazyOptional<ITachyonStorage> holder = LazyOptional.of(() -> this);

    public TachyonCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_BE.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        // --- ЛОГІКА ОНОВЛЕННЯ СВІТІННЯ ---
        BlockState currentState = this.getBlockState();
        boolean isPoweredState = currentState.getValue(TachyonCableBlock.POWERED);
        boolean hasEnergy = this.energy > 0;

        // Якщо фактична наявність енергії відрізняється від відображення блоку -> оновити блок
        if (isPoweredState != hasEnergy) {
            level.setBlock(worldPosition, currentState.setValue(TachyonCableBlock.POWERED, hasEnergy), 3);
        }

        // Якщо енергії немає, передавати нічого
        if (energy <= 0) return;

        // --- ЛОГІКА ПЕРЕДАЧІ ЕНЕРГІЇ ---
        for (Direction dir : Direction.values()) {
            if (energy <= 0) break;

            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;

            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                if (neighbor instanceof TachyonCableBlockEntity nextCable) {
                    // 1. Якщо сусід - теж кабель: вирівнюємо рівень енергії
                    if (this.energy > nextCable.energy) {
                        int totalEnergy = this.energy + nextCable.energy;
                        int targetEnergy = totalEnergy / 2;
                        int toMove = Math.min(this.energy - targetEnergy, MAX_TRANSFER);

                        if (toMove > 0) {
                            nextCable.energy += toMove;
                            this.energy -= toMove;
                            this.setChanged();
                            nextCable.setChanged();
                        }
                    }
                } else {
                    // 2. Якщо сусід - машина: просто пхаємо енергію (Push)
                    int toPush = Math.min(energy, MAX_TRANSFER);
                    int accepted = cap.receiveTacionEnergy(toPush, false); // false = не симуляція
                    if (accepted > 0) {
                        this.energy -= accepted;
                        this.setChanged();
                    }
                }
            });
        }
    }

    // --- МЕТОДИ ITachyonStorage ---

    @Override
    public int receiveTacionEnergy(int amount, boolean simulate) {
        int space = MAX_CAPACITY - energy;
        int toAdd = Math.min(amount, space);
        if (!simulate && toAdd > 0) {
            energy += toAdd;
            setChanged();
        }
        return toAdd;
    }

    @Override
    public int extractTacionEnergy(int amount, boolean simulate) {
        int toTake = Math.min(energy, amount);
        if (!simulate && toTake > 0) {
            energy -= toTake;
            setChanged();
        }
        return toTake;
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int getMaxCapacity() {
        return MAX_CAPACITY;
    }

    // --- CAPABILITIES & NBT (Збереження гри) ---

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return holder.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        holder.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.energy = tag.getInt("Energy");
    }
}