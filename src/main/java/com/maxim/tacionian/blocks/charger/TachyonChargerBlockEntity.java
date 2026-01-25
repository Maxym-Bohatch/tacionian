/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
        if (level.isClientSide) return;

        boolean changed = false;

        // --- КРОК 0: ВСМОКТУВАННЯ ЕНЕРГІЇ (Автономність) ---
        if (be.storedEnergy < be.MAX_CAPACITY) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null || neighbor instanceof TachyonChargerBlockEntity) continue;

                // Витягуємо тахіони з кабелів/сховищ
                var txCap = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite());
                if (txCap.isPresent()) {
                    int space = be.MAX_CAPACITY - be.storedEnergy;
                    int pulled = txCap.orElse(null).extractTacionEnergy(Math.min(space, 100), false);
                    if (pulled > 0) {
                        be.storedEnergy += pulled;
                        changed = true;
                    }
                }
            }
        }

        if (be.storedEnergy <= 0) {
            if (changed) be.setChanged();
            return;
        }

        // --- КРОК 1 ТА 2: РОЗДАЧА ТА КОНВЕРТАЦІЯ ---
        for (Direction dir : Direction.values()) {
            if (be.storedEnergy <= 0) break;

            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null || neighbor instanceof TachyonChargerBlockEntity) continue;

            // 1. Передача в інші тахіонні блоки
            var txCap = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite());
            if (txCap.isPresent()) {
                int toPush = Math.min(be.storedEnergy, 100);
                int accepted = txCap.orElse(null).receiveTacionEnergy(toPush, false);
                if (accepted > 0) {
                    be.storedEnergy -= accepted;
                    changed = true;
                }
            }

            // 2. КОНВЕРТАЦІЯ В RF (Forge Energy)
            if (be.storedEnergy > 0) {
                var rfCap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                if (rfCap.isPresent()) {
                    IEnergyStorage storage = rfCap.orElse(null);
                    if (storage.canReceive()) {
                        // Рахуємо доступний RF: (storedEnergy * 10 * ефективність)
                        float eff = be.getEfficiency();
                        int maxRfAvailable = (int)(be.storedEnergy * 10 * eff);
                        int toSendRF = Math.min(maxRfAvailable, 1000);

                        int acceptedRF = storage.receiveEnergy(toSendRF, false);
                        if (acceptedRF > 0) {
                            // Вираховуємо витрати Tx: (RF / 10 / ефективність)
                            // Використовуємо ceil, щоб завжди округляти вгору (податок на конвертацію)
                            int txToDrain = (int) Math.ceil((acceptedRF / 10.0) / eff);
                            be.storedEnergy = Math.max(0, be.storedEnergy - txToDrain);
                            changed = true;
                        }
                    }
                }
            }
        }

        if (changed) {
            be.setChanged();
            // Надсилаємо оновлення блоку, щоб клієнт бачив актуальну енергію
            level.sendBlockUpdated(pos, state, state, 3);
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