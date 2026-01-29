/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.blocks;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StabilizationPlateBlockEntity extends BlockEntity implements ITachyonStorage {
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 500;
    private int currentMode = 0;

    public StabilizationPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZER_PLATE_BE.get(), pos, state);
    }

    public void cycleMode() {
        this.currentMode = (this.currentMode + 1) % 4;
        setChanged();
    }

    public int getCurrentMode() { return currentMode; }

    public static void tick(Level level, BlockPos pos, BlockState state, StabilizationPlateBlockEntity be) {
        if (level.isClientSide) return;

        AABB area = new AABB(pos.above());
        level.getEntitiesOfClass(ServerPlayer.class, area).forEach(player -> {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                pEnergy.setPlateStabilized(true);

                int maxE = pEnergy.getMaxEnergy();
                int currentE = pEnergy.getEnergy();

                // ВИЗНАЧЕННЯ ПОРOГУ (Синхронізація з ядром та локалізацією)
                int safetyThreshold;
                if (pEnergy.getLevel() <= 5) {
                    safetyThreshold = (int)(maxE * 0.8f); // Новачок: ліміт 80%
                } else {
                    safetyThreshold = switch (be.currentMode) {
                        case 0 -> (int)(maxE * 0.75f); // Safe mode (75%)
                        case 1 -> (int)(maxE * 0.40f); // Balanced (40%)
                        case 3 -> (int)(maxE * 2.0f);  // Unrestricted (200%)
                        default -> maxE;               // Performance (100%)
                    };
                }

                // Блокуємо регенерацію, якщо енергія вище ліміту плити
                pEnergy.setRegenBlocked(currentE >= safetyThreshold && be.currentMode != 3);

                boolean isManualDrain = player.isCrouching();
                int targetEnergy = isManualDrain ? 0 : safetyThreshold;

                int actuallyExtracted = 0;
                if (currentE > targetEnergy) {
                    int excess = currentE - targetEnergy;
                    int drainRate = isManualDrain ? 80 : 40;
                    int toExtract = drainRate + (excess / 10);

                    actuallyExtracted = pEnergy.extractEnergyPure(toExtract, false);
                    int accepted = be.receiveTacionEnergy(actuallyExtracted, false);

                    if (level.getGameTime() % 10 == 0 && actuallyExtracted > 0) {
                        float pitch = isManualDrain ? 0.7f : 1.1f;
                        level.playSound(null, pos, ModSounds.TACHYON_HUM.get(), SoundSource.BLOCKS, 0.2f, pitch);
                    }

                    if (accepted < actuallyExtracted) {
                        MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, actuallyExtracted - accepted));
                    }

                    pEnergy.sync(player);
                    be.setChanged();
                }

                // Візуальний ефект
                if (actuallyExtracted > 0 || (currentE > maxE && be.currentMode == 3)) {
                    spawnBeamParticles(level, pos, player);
                }
            });
        });

        if (be.storedEnergy > 0) {
            pushEnergyToNeighbors(level, pos, be);
        }
    }

    private static void spawnBeamParticles(Level level, BlockPos pos, ServerPlayer player) {
        Vec3 playerFeetPos = player.position().add(0, 0.1, 0);
        Vec3 plateCenterPos = Vec3.atCenterOf(pos).add(0, 0.1, 0);
        double distance = plateCenterPos.distanceTo(playerFeetPos);
        int particles = Math.max(2, (int)(distance * 2));

        for (int i = 0; i < particles; i++) {
            double prg = (double)i / particles;
            double x = plateCenterPos.x + (playerFeetPos.x - plateCenterPos.x) * prg;
            double y = plateCenterPos.y + (playerFeetPos.y - plateCenterPos.y) * prg;
            double z = plateCenterPos.z + (playerFeetPos.z - plateCenterPos.z) * prg;

            ((ServerLevel)level).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x + (level.random.nextDouble()-0.5)*0.15,
                    y + (level.random.nextDouble()-0.5)*0.15,
                    z + (level.random.nextDouble()-0.5)*0.15,
                    1, 0, 0, 0, 0.02);
        }
    }

    private static void pushEnergyToNeighbors(Level level, BlockPos pos, StabilizationPlateBlockEntity be) {
        for (Direction dir : Direction.values()) {
            if (be.storedEnergy <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null && !(neighbor instanceof StabilizationPlateBlockEntity)) {
                neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                    int pushed = cap.receiveTacionEnergy(Math.min(be.storedEnergy, 1000), false);
                    if (pushed > 0) {
                        be.storedEnergy -= pushed;
                        be.setChanged();
                    }
                });
            }
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.storedEnergy = nbt.getInt("TachyonEnergy");
        this.currentMode = nbt.getInt("StabilizationMode");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("TachyonEnergy", storedEnergy);
        nbt.putInt("StabilizationMode", currentMode);
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        int canReceive = Math.min(amount, MAX_CAPACITY - storedEnergy);
        if (!simulate && canReceive > 0) { storedEnergy += canReceive; setChanged(); }
        return canReceive;
    }

    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int canExtract = Math.min(amount, storedEnergy);
        if (!simulate && canExtract > 0) { storedEnergy -= canExtract; setChanged(); }
        return canExtract;
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return LazyOptional.of(() -> this).cast();
        return super.getCapability(cap, side);
    }
}