package com.maxim.tacionian.blocks.cable;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.TachyonEnergyStorage;
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

public class TachyonCableBlockEntity extends BlockEntity implements ITachyonStorage {
    // Буфер кабелю: 1000 Tx ємність, 200 Tx вхід/вихід за тік
    private final TachyonEnergyStorage energyStorage = new TachyonEnergyStorage(1000, 200, 200);
    private final LazyOptional<ITachyonStorage> holder = LazyOptional.of(() -> this);
    private int lastLightLevel = -1;

    public TachyonCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TachyonCableBlockEntity be) {
        if (level.isClientSide) return;

        // ЛОГІКА ПЕРЕДАЧІ (Сполучені посудини)
        if (be.getEnergy() > 0) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null) continue;

                neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(other -> {
                    // Якщо у сусіда менше енергії — віддаємо частину
                    if (be.getEnergy() > other.getEnergy()) {
                        int diff = be.getEnergy() - other.getEnergy();
                        int toSend = Math.min(be.energyStorage.getMaxExtract(), diff / 2);

                        if (toSend > 0) {
                            int accepted = other.receiveTacionEnergy(toSend, false);
                            be.extractTacionEnergy(accepted, false);
                        }
                    }
                });
            }
        }

        // ВІЗУАЛІЗАЦІЯ (Світло)
        if (level.getGameTime() % 20 == 0) {
            int targetLight = be.getEnergy() > 0 ? Math.min(3, (be.getEnergy() / 333) + 1) : 0;
            if (be.lastLightLevel != targetLight) {
                be.lastLightLevel = targetLight;
                level.setBlock(pos, state.setValue(TachyonCableBlock.LIGHT_LEVEL, targetLight)
                        .setValue(TachyonCableBlock.POWERED, targetLight > 0), 3);
            }
        }
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) { return energyStorage.receiveEnergy(amount, simulate); }
    @Override public int extractTacionEnergy(int amount, boolean simulate) { return energyStorage.extractEnergy(amount, simulate); }
    @Override public int getEnergy() { return energyStorage.getEnergyStored(); }
    @Override public int getMaxCapacity() { return energyStorage.getMaxEnergyStored(); }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return holder.cast();
        return super.getCapability(cap, side);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.putInt("Energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergy(tag.getInt("Energy"));
    }
}