/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.energy.PlayerEnergy;
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
    private int mode = 0; // 0: Safe, 1: Balanced, 2: Performance, 3: Unrestricted
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 50000; // Повернено великий об'єм

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> rfOutputHolder = LazyOptional.of(this::createRFStorage);

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    private IEnergyStorage createRFStorage() {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int txNeeded = (int) Math.ceil(maxExtract / 10.0);
                int txToExtract = Math.min(storedEnergy, txNeeded);
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
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessEnergyInterfaceBlockEntity be) {
        if (level.isClientSide) return;

        // 1. ПОШУК ГРАВЦІВ ТА СТАБІЛІЗАЦІЯ
        int radius = TacionianConfig.INTERFACE_RADIUS.get();
        AABB area = new AABB(pos).inflate(radius);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player p : players) {
            if (p instanceof ServerPlayer sp) {
                sp.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(cap -> {
                    if (cap.isDisconnected() || cap.isRemoteAccessBlocked()) return;

                    // АКТИВАЦІЯ СТАБІЛІЗАЦІЇ (Працює як зовнішній запобіжник)
                    cap.setInterfaceStabilized(true);

                    // ЛОГІКА ВЗАЄМОДІЇ ЗАЛЕЖНО ВІД РЕЖИМУ
                    processPlayerInteraction(be, cap, sp);
                });
            }
        }

        // 2. АВТОМАТИЧНА РОЗДАЧА ЕНЕРГІЇ В МЕРЕЖУ (Push)
        if (be.storedEnergy > 0) {
            be.distributeEnergy(level, pos);
        }
    }

    private static void processPlayerInteraction(WirelessEnergyInterfaceBlockEntity be, PlayerEnergy cap, ServerPlayer sp) {
        float ratio = cap.getEnergyFraction();

        // Пороги спрацювання залежно від режиму блоку
        float drainThreshold = switch (be.mode) {
            case 0 -> 0.75f; // Safe: тримає ядро на 75%
            case 1 -> 0.90f; // Balanced: тримає на 90%
            case 2 -> 1.50f; // Performance: дозволяє перевантаження до 150%
            case 3 -> 1.95f; // Unrestricted: качає майже до вибуху
            default -> 0.85f;
        };

        // ВИКАЧУВАННЯ НАДЛИШКУ (Захист від вибуху)
        if (ratio > drainThreshold && be.storedEnergy < be.MAX_CAPACITY) {
            int toDrain = Math.min(be.MAX_CAPACITY - be.storedEnergy, 200);
            int extracted = cap.extractEnergyPure(toDrain, false);
            be.storedEnergy += extracted;
            be.setChanged();

            // Ефект "всмоктування" енергії
            if (sp.tickCount % 5 == 0) {
                ((ServerLevel)sp.level()).sendParticles(ParticleTypes.REVERSE_PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
            }
        }

        // ЗВОРОТНЕ ПІДЖИВЛЕННЯ (Якщо гравцеві мало енергії, а в блоці є запас)
        if (ratio < (drainThreshold - 0.1f) && be.storedEnergy > 0) {
            int toGive = Math.min(be.storedEnergy, 50);
            int accepted = cap.receiveEnergyPure(toGive, false);
            be.storedEnergy -= accepted;
            be.setChanged();
        }
    }

    private void distributeEnergy(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (storedEnergy <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            // 1. Пріоритет - Тахіонні кабелі (TX -> TX)
            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(txCap -> {
                int accepted = txCap.receiveTacionEnergy(Math.min(storedEnergy, 1000), false);
                this.storedEnergy -= accepted;
                this.setChanged();
            });

            // 2. Конвертація в RF з нарахуванням XP
            if (storedEnergy > 0) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(rfCap -> {
                    if (rfCap.canReceive()) {
                        int toConvert = Math.min(storedEnergy, 500);
                        int acceptedRF = rfCap.receiveEnergy(toConvert * 10, false);
                        int consumedTX = (int) Math.ceil(acceptedRF / 10.0);

                        if (consumedTX > 0) {
                            this.storedEnergy -= consumedTX;
                            this.setChanged();

                            // Повертаємо досвід найближчому гравцю
                            Player p = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16, false);
                            if (p instanceof ServerPlayer sp) {
                                sp.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(c -> c.addExperience(consumedTX * 0.25f, sp));
                            }
                        }
                    }
                });
            }
        }
    }

    public void cycleMode(Player player) {
        this.mode = (this.mode + 1) % 4;
        setChanged();
        player.displayClientMessage(getModeName(), true);
    }

    public Component getModeName() {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe");
            case 1 -> Component.translatable("mode.tacionian.balanced");
            case 2 -> Component.translatable("mode.tacionian.performance");
            case 3 -> Component.translatable("mode.tacionian.unrestricted");
            default -> Component.literal("Unknown");
        };
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        int accepted = Math.min(amount, MAX_CAPACITY - storedEnergy);
        if (!simulate) { storedEnergy += accepted; setChanged(); }
        return accepted;
    }
    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int toExtract = Math.min(amount, storedEnergy);
        if (!simulate) { storedEnergy -= toExtract; setChanged(); }
        return toExtract;
    }
    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.TACHYON_STORAGE) return tachyonHolder.cast();
        if (cap == ForgeCapabilities.ENERGY) return rfOutputHolder.cast();
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