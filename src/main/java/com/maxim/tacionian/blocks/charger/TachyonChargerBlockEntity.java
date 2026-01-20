package com.maxim.tacionian.blocks.charger;

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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlockEntity extends BlockEntity implements ITachyonStorage {
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 500;

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> rfHolder = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return TachyonChargerBlockEntity.this.receiveTacionEnergy(maxReceive / 10, simulate) * 10;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return TachyonChargerBlockEntity.this.extractTacionEnergy(maxExtract / 10, simulate) * 10;
        }
        @Override public int getEnergyStored() { return storedEnergy * 10; }
        @Override public int getMaxEnergyStored() { return MAX_CAPACITY * 10; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    });

    public TachyonChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER_BE.get(), pos, state);
    }

    @Override
    public int receiveTacionEnergy(int amount, boolean simulate) {
        int space = MAX_CAPACITY - storedEnergy;
        int toReceive = Math.min(amount, space);
        if (!simulate && toReceive > 0) {
            storedEnergy += toReceive;
            setChanged();
        }
        return toReceive;
    }

    @Override
    public int extractTacionEnergy(int amount, boolean simulate) {
        int toExtract = Math.min(storedEnergy, amount);
        if (!simulate && toExtract > 0) {
            storedEnergy -= toExtract;
            setChanged();
        }
        return toExtract;
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return tachyonHolder.cast();
        if (cap == ForgeCapabilities.ENERGY) return rfHolder.cast();
        return super.getCapability(cap, side);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TachyonChargerBlockEntity be) {
        if (level.isClientSide || be.storedEnergy <= 0) return;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            // Перевіряємо TX (Твій мод)
            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(txCap -> {
                int toTransfer = Math.min(be.storedEnergy, 20); // Ліміт передачі за тік
                int accepted = txCap.receiveTacionEnergy(toTransfer, false);
                be.extractTacionEnergy(accepted, false);
            });

            // Перевіряємо RF (Forge) - ВАЖЛИВО: перевіряємо навіть якщо TX спрацював
            if (be.storedEnergy > 0) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(rfCap -> {
                    if (rfCap.canReceive()) {
                        int rfToGive = Math.min(be.storedEnergy * 10, 200); // 1 TX = 10 RF
                        int acceptedRF = rfCap.receiveEnergy(rfToGive, false);
                        be.extractTacionEnergy(acceptedRF / 10, false);
                    }
                });
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putInt("StoredEnergy", storedEnergy);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.storedEnergy = nbt.getInt("StoredEnergy");
    }
}