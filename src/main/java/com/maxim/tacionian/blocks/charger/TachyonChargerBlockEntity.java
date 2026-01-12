package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlockEntity extends BlockEntity {
    private int storedEnergy = 0;
    private final int capacity = 50000;

    public TachyonChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER_BE.get(), pos, state);
    }

    public int receiveEnergySafe(int amount, boolean simulate) {
        int space = capacity - storedEnergy;
        int toReceive = Math.min(amount, space);
        if (!simulate) {
            storedEnergy += toReceive;
            setChanged();
        }
        return toReceive;
    }

    // СТАНДАРТНИЙ ЕНЕРГО-ПОРТ (Для кабелів Mekanism, Thermal і т.д.)
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return receiveEnergySafe(maxReceive, simulate); }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            int toExtract = Math.min(storedEnergy, maxExtract);
            if (!simulate) { storedEnergy -= toExtract; setChanged(); }
            return toExtract;
        }
        @Override public int getEnergyStored() { return storedEnergy; }
        @Override public int getMaxEnergyStored() { return capacity; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    });

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyHandler.cast();
        return super.getCapability(cap, side);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TachyonChargerBlockEntity be) {
        if (level.isClientSide || be.storedEnergy <= 0) return;

        // Автоматична роздача енергії в сусідні RF-машини/кабелі
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(cap -> {
                    if (cap.canReceive()) {
                        int sent = cap.receiveEnergy(Math.min(be.storedEnergy, 1000), false);
                        if (sent > 0) {
                            be.storedEnergy -= sent;
                            be.setChanged();
                        }
                    }
                });
            }
        }
    }
}