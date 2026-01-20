package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.maxim.tacionian.blocks.storage.EnergyReservoirBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WirelessEnergyInterfaceBlockEntity extends BlockEntity implements ITachyonStorage {
    private int mode = 0;
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 1000;

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> rfHolder = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return WirelessEnergyInterfaceBlockEntity.this.extractTacionEnergy(maxExtract / 10, simulate) * 10;
        }
        @Override public int getEnergyStored() { return storedEnergy * 10; }
        @Override public int getMaxEnergyStored() { return MAX_CAPACITY * 10; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    });

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) {
        int toAdd = Math.min(amount, MAX_CAPACITY - storedEnergy);
        if (!simulate && toAdd > 0) { storedEnergy += toAdd; setChanged(); }
        return toAdd;
    }

    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int toTake = Math.min(storedEnergy, amount);
        if (!simulate && toTake > 0) { storedEnergy -= toTake; setChanged(); }
        return toTake;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessEnergyInterfaceBlockEntity be) {
        if (level.isClientSide) return;

        AABB area = new AABB(pos).inflate(20);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);
        int threshold = switch (be.mode) { case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 0; };

        for (Player player : players) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                    pEnergy.setRemoteStabilized(true);
                    boolean hasConnections = checkAnyConnections(level, pos);

                    if (hasConnections) {
                        pEnergy.setRemoteNoDrain(false);
                        // Якщо перевантажений - рятуємо гравця (даємо трохи досвіду за стабілізацію)
                        if (pEnergy.isOverloaded()) {
                            int extracted = pEnergy.extractEnergyPure(40, false);
                            pEnergy.addExperience(extracted * 0.05f, serverPlayer);
                            int leftover = extracted - be.receiveTacionEnergy(extracted, false);
                            if (leftover > 0) MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, leftover));
                        } else if (pEnergy.getEnergyPercent() > threshold && be.storedEnergy < be.MAX_CAPACITY) {
                            // Звичайне переливання (без досвіду, щоб не було дюпу)
                            be.receiveTacionEnergy(pEnergy.extractEnergyPure(50, false), false);
                        }
                        processEnergyTransfer(level, pos, be, serverPlayer);
                    } else {
                        // Режим антени (скид у повітря)
                        if (pEnergy.getEnergyPercent() > threshold && level.getGameTime() % 20 == 0) {
                            int waste = pEnergy.extractEnergyPure(25, false);
                            MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, waste));
                        }
                    }
                    if (level.getGameTime() % 10 == 0) pEnergy.sync(serverPlayer);
                });
            }
        }
    }

    private static void processEnergyTransfer(Level level, BlockPos pos, WirelessEnergyInterfaceBlockEntity be, ServerPlayer player) {
        if (be.storedEnergy <= 0) return;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            // Передача в Tachyon блоки (Машини/Кабелі)
            neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).ifPresent(cap -> {
                if (!(neighbor instanceof EnergyReservoirBlockEntity) && cap.getEnergy() < cap.getMaxCapacity()) {
                    int accepted = cap.receiveTacionEnergy(Math.min(be.storedEnergy, 25), false);
                    be.extractTacionEnergy(accepted, false);
                    // Додаємо досвід гравцю, бо енергія пішла в роботу!
                    player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(e -> e.addExperience(accepted * 0.2f, player));
                }
            });

            // Передача в RF (Енергія конвертується)
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(cap -> {
                if (cap.canReceive()) {
                    int acceptedRf = cap.receiveEnergy(Math.min(be.storedEnergy * 10, 250), false);
                    be.extractTacionEnergy(acceptedRf / 10, false);
                    player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(e -> e.addExperience((acceptedRf / 10f) * 0.2f, player));
                }
            });
        }
    }

    private static boolean checkAnyConnections(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null && (neighbor.getCapability(ModCapabilities.TACHYON_STORAGE).isPresent() || neighbor.getCapability(ForgeCapabilities.ENERGY).isPresent())) return true;
        }
        return false;
    }

    private static boolean checkMachinesNeedPower(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                boolean rf = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).map(c -> c.canReceive() && c.receiveEnergy(10, true) > 0).orElse(false);
                boolean tx = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite()).map(c -> c.getEnergy() < c.getMaxCapacity() * 0.9).orElse(false);
                if (rf || tx) return true;
            }
        }
        return false;
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }
    public Component getModeName() { return switch (mode) { case 0 -> Component.translatable("mode.tacionian.safe"); case 1 -> Component.translatable("mode.tacionian.balanced"); case 2 -> Component.translatable("mode.tacionian.performance"); default -> Component.translatable("mode.tacionian.unrestricted"); }; }
    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) { if (cap == ModCapabilities.TACHYON_STORAGE) return tachyonHolder.cast(); if (cap == ForgeCapabilities.ENERGY) return rfHolder.cast(); return super.getCapability(cap, side); }
    public void cycleMode(Player player) { this.mode = (this.mode + 1) % 4; if (level != null && !level.isClientSide) player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName()), true); setChanged(); }
    @Override public void invalidateCaps() { super.invalidateCaps(); tachyonHolder.invalidate(); rfHolder.invalidate(); }
    @Override protected void saveAdditional(CompoundTag nbt) { nbt.putInt("Mode", mode); nbt.putInt("StoredEnergy", storedEnergy); super.saveAdditional(nbt); }
    @Override public void load(CompoundTag nbt) { super.load(nbt); this.mode = nbt.getInt("Mode"); this.storedEnergy = nbt.getInt("StoredEnergy"); }
}