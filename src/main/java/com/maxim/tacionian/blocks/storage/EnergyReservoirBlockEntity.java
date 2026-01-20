package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnergyReservoirBlockEntity extends BlockEntity implements ITachyonStorage {
    private int energy = 0;
    private final int MAX_CAPACITY = 25000;

    private final LazyOptional<ITachyonStorage> holder = LazyOptional.of(() -> this);

    public EnergyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESERVOIR_BE.get(), pos, state);
    }

    @Override
    public int receiveTacionEnergy(int amount, boolean simulate) {
        int space = MAX_CAPACITY - energy;
        int toAdd = Math.min(amount, space);
        if (!simulate && toAdd > 0) {
            energy += toAdd;
            updateBlock();
        }
        return toAdd;
    }

    @Override
    public int extractTacionEnergy(int amount, boolean simulate) {
        int toExtract = Math.min(amount, energy);
        if (!simulate && toExtract > 0) {
            energy -= toExtract;
            updateBlock();
        }
        return toExtract;
    }

    // Тікер для автоматичної взаємодії з кабелями
    public static void tick(Level level, BlockPos pos, BlockState state, EnergyReservoirBlockEntity be) {
        if (level.isClientSide) return;

        // 1. ПРИЙОМ: Резервуар викачує енергію з кабелів/блоків, які мають енергію
        if (be.energy < be.MAX_CAPACITY) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null) continue;

                neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                    // Резервуар забирає до 100 Tx за тік (швидке завантаження)
                    int toPull = Math.min(be.MAX_CAPACITY - be.energy, 100);
                    int pulled = cap.extractTacionEnergy(toPull, false);
                    be.receiveTacionEnergy(pulled, false);
                });
            }
        }

        // 2. РОЗДАЧА: Якщо ти захочеш, щоб резервуар сам живив кабелі (опціонально)
        // Якщо це просто сховище, цей блок можна закоментувати, щоб енергія виходила тільки за запитом споживача
        /*
        if (be.energy > 0) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null) continue;
                neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                    if (cap.getEnergy() < cap.getMaxCapacity()) {
                        int toPush = Math.min(be.energy, 100);
                        int accepted = cap.receiveTacionEnergy(toPush, false);
                        be.extractTacionEnergy(accepted, false);
                    }
                });
            }
        }
        */
    }

    private void updateBlock() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public int getEnergy() { return energy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

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
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.energy = nbt.getInt("StoredTacion");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("StoredTacion", energy);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.load(tag);
        }
    }
}