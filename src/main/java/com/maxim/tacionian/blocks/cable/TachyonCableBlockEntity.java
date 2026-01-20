package com.maxim.tacionian.blocks.cable;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.TachyonNetwork;
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

public class TachyonCableBlockEntity extends BlockEntity implements ITachyonStorage {
    private TachyonNetwork network;
    private final LazyOptional<ITachyonStorage> holder = LazyOptional.of(() -> this);

    public TachyonCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_BE.get(), pos, state);
    }

    public void setNetwork(TachyonNetwork net) {
        this.network = net;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TachyonCableBlockEntity be) {
        if (level.isClientSide) return;

        if (be.network == null) {
            be.refreshNetwork();
        }

        if (be.network != null) {
            be.network.tickMaster(be, level);

            // СИНХРОНІЗАЦІЯ ВІЗУАЛУ
            boolean hasEnergy = be.network.getEnergy() > 0;
            if (state.getValue(TachyonCableBlock.POWERED) != hasEnergy) {
                // Оновлюємо стан на сервері
                level.setBlock(pos, state.setValue(TachyonCableBlock.POWERED, hasEnergy), 3);
                // Відправляємо пакет оновлення всім гравцям поруч
                level.sendBlockUpdated(pos, state, state.setValue(TachyonCableBlock.POWERED, hasEnergy), 3);
            }
        }
    }

    public void refreshNetwork() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor instanceof TachyonCableBlockEntity otherCable && otherCable.network != null) {
                if (this.network == null) {
                    this.network = otherCable.network;
                    this.network.addCable(this);
                } else if (this.network != otherCable.network) {
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

    // МЕРЕЖЕВА СИНХРОНІЗАЦІЯ (S2C)
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("powered", network != null && network.getEnergy() > 0);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            boolean isPowered = pkt.getTag().getBoolean("powered");
            if (level != null) {
                BlockState state = level.getBlockState(worldPosition);
                if (state.hasProperty(TachyonCableBlock.POWERED) && state.getValue(TachyonCableBlock.POWERED) != isPowered) {
                    level.setBlock(worldPosition, state.setValue(TachyonCableBlock.POWERED, isPowered), 3);
                }
            }
        }
    }

    // ITachyonStorage
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