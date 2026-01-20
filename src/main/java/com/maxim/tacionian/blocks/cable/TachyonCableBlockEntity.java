package com.maxim.tacionian.blocks.cable;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.TachyonNetwork;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TachyonCableBlockEntity extends BlockEntity implements ITachyonStorage {
    private TachyonNetwork network;
    private final LazyOptional<ITachyonStorage> holder = LazyOptional.of(() -> this);

    public TachyonCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_BE.get(), pos, state);
    }

    public void setNetwork(TachyonNetwork net) {
        this.network = net;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        // 1. Ініціалізація або пошук мережі
        if (network == null) {
            findOrCreateNetwork();
        }

        // Тільки один кабель з мережі (майстер) оновлює всю мережу
        // Це економить ресурси процесора в рази!
        if (network != null && network.tickMaster(this, level)) {
            // Оновлення світіння для всього сегмента можна додати тут
        }

        // Оновлення візуалу POWERED
        boolean hasEnergy = network != null && network.getEnergy() > 0;
        if (getBlockState().getValue(TachyonCableBlock.POWERED) != hasEnergy) {
            level.setBlock(worldPosition, getBlockState().setValue(TachyonCableBlock.POWERED, hasEnergy), 3);
        }
    }

    private void findOrCreateNetwork() {
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof TachyonCableBlockEntity otherCable && otherCable.network != null) {
                if (this.network == null) {
                    this.network = otherCable.network;
                    this.network.addCable(this);
                } else {
                    this.network.merge(otherCable.network);
                }
            }
        }
        if (this.network == null) {
            this.network = new TachyonNetwork();
            this.network.addCable(this);
        }
    }

    @Override
    public void setRemoved() {
        if (network != null) network.removeCable(this);
        super.setRemoved();
    }

    // Методи ITachyonStorage тепер делегують все мережі
    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        return network != null ? network.receiveEnergy(amount, simulate) : 0;
    }

    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        return network != null ? network.extractEnergy(amount, simulate) : 0;
    }

    @Override public int getEnergy() { return network != null ? network.getEnergy() : 0; }
    @Override public int getMaxCapacity() { return network != null ? network.getCapacity() : 0; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return holder.cast();
        return super.getCapability(cap, side);
    }
}