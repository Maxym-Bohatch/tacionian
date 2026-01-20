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

    public static void tick(Level level, BlockPos pos, BlockState state, TachyonCableBlockEntity be) {
        if (level.isClientSide) return;

        if (be.network == null) be.refreshNetwork();

        if (be.network != null) {
            be.network.tickMaster(be, level);

            // ВИЗУАЛЬНЕ ОНОВЛЕННЯ:
            // Кабель вважається POWERED, якщо енергія є в мережі АБО у сусіда-резервуара
            boolean hasEnergy = be.network.getEnergy() > 0;

            if (!hasEnergy) {
                for (Direction dir : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                    // Перевіряємо капабіліті тахіонів у сусіда
                    if (neighbor != null && !(neighbor instanceof TachyonCableBlockEntity)) {
                        var cap = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite());
                        if (cap.isPresent() && cap.map(ITachyonStorage::getEnergy).orElse(0) > 0) {
                            hasEnergy = true;
                            break;
                        }
                    }
                }
            }

            if (state.getValue(TachyonCableBlock.POWERED) != hasEnergy) {
                level.setBlock(pos, state.setValue(TachyonCableBlock.POWERED, hasEnergy), 3);
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

    @Override public int receiveTacionEnergy(int amount, boolean simulate) { return network != null ? network.receiveEnergy(amount, simulate) : 0; }
    @Override public int extractTacionEnergy(int amount, boolean simulate) { return network != null ? network.extractEnergy(amount, simulate) : 0; }
    @Override public int getEnergy() { return network != null ? network.getEnergy() : 0; }
    @Override public int getMaxCapacity() { return network != null ? network.getCapacity() : 0; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return holder.cast();
        return super.getCapability(cap, side);
    }
}