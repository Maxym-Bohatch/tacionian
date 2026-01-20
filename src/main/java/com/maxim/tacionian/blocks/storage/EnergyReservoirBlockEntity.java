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

    // Тік тепер майже порожній, бо TachyonNetwork сама керує переливанням
    public static void tick(Level level, BlockPos pos, BlockState state, EnergyReservoirBlockEntity be) {
        if (level.isClientSide) return;

        // Можна залишити логіку живлення машин, які стоять впритул без кабелів,
        // але для переливання по кабелях цей метод більше НЕ потрібен.
    }

    private void updateBlock() {
        setChanged();
        if (level != null && !level.isClientSide) {
            // Оновлюємо блок, щоб клієнт бачив актуальну енергію
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public int getEnergy() { return energy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return holder.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); holder.invalidate(); }

    // ВАЖЛИВО: Переконайся, що NBT теги збігаються скрізь
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
        if (pkt.getTag() != null) {
            this.load(pkt.getTag());
        }
    }
}