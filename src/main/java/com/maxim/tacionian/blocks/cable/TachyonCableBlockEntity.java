package com.maxim.tacionian.blocks.cable;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.TachyonNetwork;
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

public class TachyonCableBlockEntity extends BlockEntity implements ITachyonStorage {
    private TachyonNetwork network;
    private final LazyOptional<ITachyonStorage> holder = LazyOptional.of(() -> this);

    public TachyonCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_BE.get(), pos, state);
    }

    public void setNetwork(TachyonNetwork network) {
        this.network = network;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TachyonCableBlockEntity be) {
        if (level.isClientSide) return;

        if (be.network == null) be.refreshNetwork();

        if (be.network != null) {
            be.network.tickMaster(be, level);

            // АКТИВНА ПЕРЕДАЧА СУСІДАМ
            if (be.network.getEnergy() > 0) {
                for (Direction dir : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                    if (neighbor != null && !(neighbor instanceof TachyonCableBlockEntity)) {
                        neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(storage -> {
                            int toPush = Math.min(be.network.getEnergy(), 1000);
                            int accepted = storage.receiveTacionEnergy(toPush, false);
                            be.network.extractEnergy(accepted, false);
                        });
                    }
                }
            }

            // ЛОГІКА СВІТІННЯ (ІДЕАЛЬНА)
            int targetLight = calculateTargetLight(level, pos, be);

            if (state.getValue(TachyonCableBlock.LIGHT_LEVEL) != targetLight) {
                level.setBlock(pos, state.setValue(TachyonCableBlock.LIGHT_LEVEL, targetLight)
                        .setValue(TachyonCableBlock.POWERED, targetLight > 0), 3);
            }
        }
    }

    private static int calculateTargetLight(Level level, BlockPos pos, TachyonCableBlockEntity be) {
        // Знаходимо максимальний відсоток заряду (своя мережа або сусіди)
        double maxPercent = 0;

        // 1. Відсоток власної мережі
        if (be.network.getCapacity() > 0) {
            maxPercent = (double) be.network.getEnergy() / be.network.getCapacity();
        }

        // 2. Відсоток сусідніх баків (щоб світитися, коли бак повний)
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null && !(neighbor instanceof TachyonCableBlockEntity)) {
                var cap = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite());
                if (cap.isPresent()) {
                    ITachyonStorage storage = cap.orElse(null);
                    if (storage != null && storage.getMaxCapacity() > 0) {
                        double neighborPercent = (double) storage.getEnergy() / storage.getMaxCapacity();
                        if (neighborPercent > maxPercent) maxPercent = neighborPercent;
                    }
                }
            }
        }

        // Конвертуємо відсоток у рівень 0-3
        if (maxPercent <= 0) return 0;
        if (maxPercent < 0.34) return 1; // 1-33%
        if (maxPercent < 0.67) return 2; // 34-66%
        return 3; // 67-100%
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

    @Override public void setRemoved() { if (network != null) network.removeCable(this); super.setRemoved(); }
    @Override public int receiveTacionEnergy(int amount, boolean simulate) { return network != null ? network.receiveEnergy(amount, simulate) : 0; }
    @Override public int extractTacionEnergy(int amount, boolean simulate) { return network != null ? network.extractEnergy(amount, simulate) : 0; }
    @Override public int getEnergy() { return network != null ? network.getEnergy() : 0; }
    @Override public int getMaxCapacity() { return network != null ? network.getCapacity() : 0; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return holder.cast();
        return super.getCapability(cap, side);
    }
}