package com.maxim.tacionian.blocks.wireless;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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

import java.util.List;

public class WirelessEnergyInterfaceBlockEntity extends BlockEntity implements ITachyonStorage {
    private int mode = 0;
    private int storedEnergy = 0;
    private final int MAX_CAPACITY = 2000;

    private final LazyOptional<ITachyonStorage> tachyonHolder = LazyOptional.of(() -> new ITachyonStorage() {
        @Override public int receiveTacionEnergy(int amount, boolean simulate) { return 0; }
        @Override public int extractTacionEnergy(int amount, boolean simulate) { return WirelessEnergyInterfaceBlockEntity.this.extractTacionEnergy(amount, simulate); }
        @Override public int getEnergy() { return storedEnergy; }
        @Override public int getMaxCapacity() { return MAX_CAPACITY; }
    });

    private final LazyOptional<IEnergyStorage> rfHolder = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return WirelessEnergyInterfaceBlockEntity.this.extractTacionEnergy(maxExtract / 10, simulate) * 10; }
        @Override public int getEnergyStored() { return storedEnergy * 10; }
        @Override public int getMaxEnergyStored() { return MAX_CAPACITY * 10; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    });

    public WirelessEnergyInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_BE.get(), pos, state);
    }

    public int receiveFromPlayer(int amount, boolean simulate) {
        int toAdd = Math.min(amount, MAX_CAPACITY - storedEnergy);
        if (!simulate && toAdd > 0) { storedEnergy += toAdd; setChanged(); }
        return toAdd;
    }

    @Override public int receiveTacionEnergy(int amount, boolean simulate) { return 0; }
    @Override public int extractTacionEnergy(int amount, boolean simulate) {
        int toTake = Math.min(storedEnergy, amount);
        if (!simulate && toTake > 0) { storedEnergy -= toTake; setChanged(); }
        return toTake;
    }

    public void cycleMode(Player player) {
        this.mode = (this.mode + 1) % 4;
        setChanged();
        if (level != null && !level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName()), true);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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

                    if (pEnergy.isOverloaded()) {
                        int extracted = pEnergy.extractEnergyPure(40, false);
                        pEnergy.addExperience(extracted * 0.05f, serverPlayer);
                        int leftover = extracted - be.receiveFromPlayer(extracted, false);
                        if (leftover > 0) MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, leftover));
                    } else if (pEnergy.getEnergyPercent() > threshold) {
                        be.receiveFromPlayer(pEnergy.extractEnergyPure(50, false), false);
                    }

                    processEnergyTransfer(level, pos, be, serverPlayer);
                    if (level.getGameTime() % 10 == 0) pEnergy.sync(serverPlayer);
                });
            }
        }
    }

    private static void processEnergyTransfer(Level level, BlockPos pos, WirelessEnergyInterfaceBlockEntity be, ServerPlayer player) {
        if (be.storedEnergy <= 0) return;

        for (Direction dir : Direction.values()) {
            if (be.storedEnergy <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null || neighbor instanceof WirelessEnergyInterfaceBlockEntity) continue;

            var tachyonCap = neighbor.getCapability(ModCapabilities.TACHYON_STORAGE, dir.getOpposite());
            var rfCap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());

            tachyonCap.ifPresent(cap -> {
                int toPush = Math.min(be.storedEnergy, 200);
                int accepted = cap.receiveTacionEnergy(toPush, false);
                be.extractTacionEnergy(accepted, false);
            });

            if (be.storedEnergy > 0) {
                rfCap.ifPresent(cap -> {
                    if (cap.canReceive()) {
                        int toPushRF = Math.min(be.storedEnergy * 10, 2000);
                        int acceptedRF = cap.receiveEnergy(toPushRF, false);
                        int usedTX = acceptedRF / 10;

                        if (usedTX > 0) {
                            be.extractTacionEnergy(usedTX, false);
                            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(e -> {
                                e.addExperience(usedTX * 0.15f, player);

                                // ВІЗУАЛ ПЕРЕДАЧІ
                                if (level.getGameTime() % 5 == 0) {
                                    ((ServerLevel)level).sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 2, 0.1, 0.1, 0.1, 0.05);
                                    ((ServerLevel)level).sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.05);
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override public int getEnergy() { return storedEnergy; }
    @Override public int getMaxCapacity() { return MAX_CAPACITY; }
    public Component getModeName() { return switch (mode) { case 0 -> Component.translatable("mode.tacionian.safe"); case 1 -> Component.translatable("mode.tacionian.balanced"); case 2 -> Component.translatable("mode.tacionian.performance"); default -> Component.translatable("mode.tacionian.unrestricted"); }; }
    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) { if (cap == ModCapabilities.TACHYON_STORAGE) return tachyonHolder.cast(); if (cap == ForgeCapabilities.ENERGY) return rfHolder.cast(); return super.getCapability(cap, side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); tachyonHolder.invalidate(); rfHolder.invalidate(); }
    @Override protected void saveAdditional(CompoundTag nbt) { super.saveAdditional(nbt); nbt.putInt("Mode", mode); nbt.putInt("StoredEnergy", storedEnergy); }
    @Override public void load(CompoundTag nbt) { super.load(nbt); this.mode = nbt.getInt("Mode"); this.storedEnergy = nbt.getInt("StoredEnergy"); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) { if (pkt.getTag() != null) load(pkt.getTag()); }
}