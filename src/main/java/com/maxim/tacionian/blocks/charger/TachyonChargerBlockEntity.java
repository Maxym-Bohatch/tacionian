package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlockEntity extends BlockEntity implements ITachyonStorage {
    protected int storedEnergy = 0;
    protected final int MAX_CAPACITY = 5000;

    public TachyonChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER_BE.get(), pos, state);
    }

    // Для сейф-версії ми перекриємо це в іншому класі
    protected float getEfficiency() {
        return 0.9f;
    }

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> this);

    private final LazyOptional<IEnergyStorage> rfHolder = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            int txToExtract = Math.min(storedEnergy, (int)((maxExtract / 10) / getEfficiency()));
            if (!simulate && txToExtract > 0) {
                storedEnergy -= txToExtract;
                setChanged();
            }
            return (int)(txToExtract * 10 * getEfficiency());
        }
        @Override public int getEnergyStored() { return (int)(storedEnergy * 10 * getEfficiency()); }
        @Override public int getMaxEnergyStored() { return MAX_CAPACITY * 10; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    });

    public static void tick(Level level, BlockPos pos, BlockState state, TachyonChargerBlockEntity be) {
        if (level.isClientSide || be.storedEnergy <= 0) return;

        for (Direction dir : Direction.values()) {
            if (be.storedEnergy <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null && !(neighbor instanceof TachyonChargerBlockEntity)) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(cap -> {
                    if (cap.canReceive()) {
                        int toSendRF = Math.min(be.storedEnergy * 10, 1000);
                        int acceptedRF = cap.receiveEnergy((int)(toSendRF * be.getEfficiency()), false);
                        int txToDrain = (int)((acceptedRF / 10) / be.getEfficiency());
                        be.storedEnergy -= Math.max(0, txToDrain);
                        be.setChanged();
                    }
                });
            }
        }
    }

    // Метод для вилучення енергії назад (Shift+ПКМ)
    public void handlePlayerExtraction(ServerPlayer player) {
        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            int toTransfer = Math.min(storedEnergy, 100);
            if (toTransfer > 0) {
                int accepted = pEnergy.receiveEnergyPure(toTransfer, false);
                this.storedEnergy -= accepted;
                setChanged();
                pEnergy.sync(player);
            }
        });
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        int toReceive = Math.min(amount, MAX_CAPACITY - storedEnergy);
        if (!simulate && toReceive > 0) { storedEnergy += toReceive; setChanged(); }
        return toReceive;
    }

    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int toTake = Math.min(storedEnergy, amount);
        if (!simulate && toTake > 0) { storedEnergy -= toTake; setChanged(); }
        return toTake;
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return tachyonHolder.cast();
        if (cap == ForgeCapabilities.ENERGY) return rfHolder.cast();
        return super.getCapability(cap, side);
    }

    @Override protected void saveAdditional(CompoundTag nbt) { nbt.putInt("energy", storedEnergy); super.saveAdditional(nbt); }
    @Override public void load(CompoundTag nbt) { super.load(nbt); this.storedEnergy = nbt.getInt("energy"); }
}