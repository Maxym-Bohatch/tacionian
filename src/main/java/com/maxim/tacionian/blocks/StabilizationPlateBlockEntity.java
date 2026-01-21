package com.maxim.tacionian.blocks;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StabilizationPlateBlockEntity extends BlockEntity implements ITachyonStorage {
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 1000;

    public StabilizationPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZER_PLATE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StabilizationPlateBlockEntity be) {
        if (level.isClientSide || be.storedEnergy <= 0) return;

        // Передача енергії в сусідні тахіонні кабелі
        for (Direction dir : Direction.values()) {
            if (be.storedEnergy <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                int toPush = Math.min(be.storedEnergy, 50);
                int accepted = cap.receiveTacionEnergy(toPush, false);
                if (accepted > 0) {
                    be.storedEnergy -= accepted;
                    be.setChanged();
                }
            });
        }
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        int canReceive = Math.min(amount, MAX_CAPACITY - storedEnergy);
        if (!simulate) { storedEnergy += canReceive; setChanged(); }
        return canReceive;
    }

    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int canExtract = Math.min(amount, storedEnergy);
        if (!simulate) { storedEnergy -= canExtract; setChanged(); }
        return canExtract;
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return LazyOptional.of(() -> this).cast();
        return super.getCapability(cap, side);
    }
}