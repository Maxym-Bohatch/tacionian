package com.maxim.tacionian.energy;

import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.level.Level;

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;
    private float fractionalExperience = 0.0f;
    private int customColor = -1;

    private int stabilizedTimer = 0;
    private int interfaceStabilizedTimer = 0;
    private int plateStabilizedTimer = 0;
    private int remoteNoDrainTimer = 0;

    private boolean disconnected = false;
    private boolean regenBlocked = false;

    public void setEnergy(int amount) { this.energy = Math.max(0, amount); }
    public void setLevel(int level) { this.level = Math.min(Math.max(1, level), TacionianConfig.MAX_LEVEL.get()); }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    public void setFractionalExperience(float f) { this.fractionalExperience = f; }
    public void setCustomColor(int color) { this.customColor = color; }

    public void setDisconnected(boolean state) { this.disconnected = state; }
    public boolean isDisconnected() { return disconnected; }
    public void setRegenBlocked(boolean state) { this.regenBlocked = state; }
    public boolean isRegenBlocked() { return regenBlocked; }

    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getCustomColor() { return customColor; }
    public int getEnergyPercent() { return (int)(getEnergyFraction() * 100); }

    public int getMaxEnergy() {
        return 1000 + (level - 1) * TacionianConfig.ENERGY_PER_LEVEL.get();
    }

    public int getRequiredExp() { return level < 20 ? 500 + (level * 100) : 2500 + (level * 250); }
    public int getRegenRate() { return (int) (TacionianConfig.BASE_REGEN.get() + (level * 2.0)); }
    public float getEnergyFraction() { return getMaxEnergy() > 0 ? (float) energy / getMaxEnergy() : 0; }

    public boolean isOverloaded() {
        // Поріг 90%, щоб HUD не вібрував при Safe-режимі (75%)
        float threshold = (this.level <= TacionianConfig.NOVICE_LEVEL_THRESHOLD.get()) ? 0.95f : 0.90f;
        return getEnergyFraction() >= threshold;
    }
    public boolean isCriticalLow() { return getEnergyFraction() < 0.05f; }

    public int receiveEnergyPure(int amount, boolean simulate) {
        int space = Math.max(0, getMaxEnergy() - this.energy);
        int toAdd = Math.min(amount, space);
        if (!simulate) this.energy += toAdd;
        return toAdd;
    }

    public int extractEnergyPure(int amount, boolean simulate) {
        int toExt = Math.min(amount, this.energy);
        if (!simulate) this.energy -= toExt;
        return toExt;
    }

    public void addExperience(float amount, ServerPlayer player) {
        if (amount <= 0 || this.level >= TacionianConfig.MAX_LEVEL.get()) return;
        this.fractionalExperience += amount;
        if (this.fractionalExperience >= 1.0f) {
            int wholeExp = (int) this.fractionalExperience;
            this.experience += wholeExp;
            this.fractionalExperience -= wholeExp;
            while (this.experience >= getRequiredExp() && this.level < TacionianConfig.MAX_LEVEL.get()) {
                this.experience -= getRequiredExp();
                this.level++;
                if (player != null) player.sendSystemMessage(Component.translatable("message.tacionian.level_up", this.level).withStyle(ChatFormatting.AQUA));
            }
            if (player != null) this.sync(player);
        }
    }

    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        MinecraftForge.EVENT_BUS.post(new TachyonEnergyUpdateEvent(player, this));

        if (stabilizedTimer > 0) stabilizedTimer--;
        if (interfaceStabilizedTimer > 0) interfaceStabilizedTimer--;
        if (plateStabilizedTimer > 0) plateStabilizedTimer--;

        boolean wasRemoteActive = remoteNoDrainTimer > 0;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;
        if (wasRemoteActive && remoteNoDrainTimer <= 0) this.sync(serverPlayer);

        if (getEnergy() >= getMaxEnergy() && !player.isCreative()) {
            if (!isStabilized() && !isInterfaceStabilized() && !isPlateStabilized()) {
                player.level().explode(null, player.getX(), player.getY(), player.getZ(), 5.0f, true, Level.ExplosionInteraction.BLOCK);
                player.hurt(player.damageSources().generic(), Float.MAX_VALUE);
                this.energy = 0;
                this.sync(serverPlayer);
                return;
            }
        }

        // --- ЛОГІКА РЕГЕНЕРАЦІЇ ---
        if (player.tickCount % 20 == 0) {
            int regenTarget = (this.level < TacionianConfig.NOVICE_LEVEL_THRESHOLD.get()) ? (int)(getMaxEnergy() * 0.9f) : getMaxEnergy();

            if (!regenBlocked && this.energy < regenTarget) {
                // Якщо стабілізатор активний, дозволяємо реген ТІЛЬКИ до 15% (мінімум виживання)
                // Це не дає йому штовхати енергію до 720+ при активованих режимах 40% чи 75%
                if (remoteNoDrainTimer <= 0 || this.energy < (getMaxEnergy() * 0.15f)) {
                    receiveEnergyPure(getRegenRate(), false);
                    this.sync(serverPlayer);
                }
            }
        }
    }

    public void setStabilized(boolean v) { if (v) this.stabilizedTimer = 20; }
    public boolean isStabilized() { return stabilizedTimer > 0; }
    public void setInterfaceStabilized(boolean v) { if (v) this.interfaceStabilizedTimer = 20; }
    public boolean isInterfaceStabilized() { return interfaceStabilizedTimer > 0; }
    public void setPlateStabilized(boolean v) { if (v) this.plateStabilizedTimer = 20; }
    public boolean isPlateStabilized() { return plateStabilizedTimer > 0; }
    public void setRemoteNoDrain(boolean v) { if (v) this.remoteNoDrainTimer = 15; }
    public boolean isRemoteNoDrain() {return this.remoteNoDrainTimer > 0;}

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("level", level);
        nbt.putInt("exp", experience);
        nbt.putFloat("fractionalExp", fractionalExperience);
        nbt.putInt("customColor", customColor);
        nbt.putBoolean("disconnected", disconnected);
        nbt.putBoolean("regenBlocked", regenBlocked);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("exp");
        this.fractionalExperience = nbt.getFloat("fractionalExp");
        this.customColor = nbt.contains("customColor") ? nbt.getInt("customColor") : -1;
        this.disconnected = nbt.getBoolean("disconnected");
        this.regenBlocked = nbt.getBoolean("regenBlocked");
    }

    public static class TachyonEnergyUpdateEvent extends Event {
        public final Player player; public final PlayerEnergy energy;
        public TachyonEnergyUpdateEvent(Player p, PlayerEnergy e) { this.player = p; this.energy = e; }
    }
}