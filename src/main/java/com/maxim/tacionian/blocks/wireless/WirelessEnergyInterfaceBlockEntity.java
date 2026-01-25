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

package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessEnergyInterfaceBlockEntity extends BlockEntity implements ITachyonStorage {
    private int mode = 0;
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 2000;

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> rfHolder = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            int txToExtract = Math.min(storedEnergy, maxExtract / 10);
            if (!simulate && txToExtract > 0) {
                storedEnergy -= txToExtract;
                setChanged();
            }
            return txToExtract * 10;
        }
        @Override public int getEnergyStored() { return storedEnergy * 10; }
        @Override public int getMaxEnergyStored() { return MAX_CAPACITY * 10; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    });

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    public void cycleMode(Player player) {
        this.mode = (this.mode + 1) % 4;
        setChanged();
        if (level != null && !level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.tacionian.mode_switched",
                    getModeName(), TacionianConfig.INTERFACE_RADIUS.get()), true);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Component getModeName() {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe");
            case 1 -> Component.translatable("mode.tacionian.balanced");
            case 2 -> Component.translatable("mode.tacionian.performance");
            default -> Component.translatable("mode.tacionian.unrestricted");
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessEnergyInterfaceBlockEntity be) {
        if (level.isClientSide) return;

        int radius = TacionianConfig.INTERFACE_RADIUS.get();
        AABB area = new AABB(pos).inflate(radius);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        int threshold = switch (be.mode) { case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 0; };

        boolean changed = false;
        for (Player player : players) {
            if (player instanceof ServerPlayer serverPlayer) {
                var cap = serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY);
                if (cap.isPresent()) {
                    var pEnergy = cap.orElse(null);

                    // ПЕРЕВІРКА: Якщо гравець від'єднався від мережі - блок його ігнорує
                    if (pEnergy.isDisconnected()) continue;

                    pEnergy.setInterfaceStabilized(true);

                    if (pEnergy.getEnergyPercent() > threshold || pEnergy.isOverloaded()) {
                        int toExtract = 50;
                        int extracted = pEnergy.extractEnergyPure(toExtract, false);
                        be.receiveFromPlayer(extracted, false);
                        changed = true;
                    }

                    if (level.getGameTime() % 10 == 0) pEnergy.sync(serverPlayer);
                }
            }
        }
    }

    private void processEnergyTransfer(Level level, BlockPos pos, @Nullable ServerPlayer player) {
        if (storedEnergy <= 0) return;

        for (Direction dir : Direction.values()) {
            if (storedEnergy <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null || neighbor instanceof WirelessEnergyInterfaceBlockEntity) continue;

            // Передача в тахіонні кабелі (досвід не даємо, це просто транспорт)
            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(txCap -> {
                int toPush = Math.min(storedEnergy, 100);
                int accepted = txCap.receiveTacionEnergy(toPush, false);
                if (accepted > 0) {
                    this.extractTacionEnergy(accepted, false);
                }
            });

            // КОНВЕРТАЦІЯ В RF (ОСЬ ТУТ ДАЄМО ДОСВІД)
            if (storedEnergy > 0) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(rfCap -> {
                    if (rfCap.canReceive()) {
                        int acceptedRF = rfCap.receiveEnergy(Math.min(storedEnergy * 10, 1000), false);
                        int usedTx = (int) Math.ceil(acceptedRF / 10.0);

                        if (usedTx > 0) {
                            this.extractTacionEnergy(usedTx, false);

                            // Нараховуємо досвід найближчому гравцю за КОРИСНУ конвертацію
                            if (player != null) {
                                player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY)
                                        .ifPresent(e -> e.addExperience(usedTx * 0.15f, player));
                            }

                            if (level instanceof ServerLevel sl && level.random.nextFloat() < 0.2f) {
                                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                        pos.relative(dir).getX() + 0.5, pos.relative(dir).getY() + 0.5, pos.relative(dir).getZ() + 0.5,
                                        2, 0.1, 0.1, 0.1, 0.02);
                            }
                        }
                    }
                });
            }
        }
    }

    public int receiveFromPlayer(int amount, boolean simulate) {
        int space = MAX_CAPACITY - storedEnergy;
        int toAdd = Math.min(amount, space);
        if (!simulate && toAdd > 0) { storedEnergy += toAdd; setChanged(); }
        return toAdd;
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) { return 0; }
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

    @Override protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("StoredEnergy", storedEnergy);
        nbt.putInt("Mode", mode);
    }

    @Override public void load(CompoundTag nbt) {
        super.load(nbt);
        this.storedEnergy = nbt.getInt("StoredEnergy");
        this.mode = nbt.getInt("Mode");
    }
}