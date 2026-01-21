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
    private int remoteStabilizedTimer = 0;
    private int remoteNoDrainTimer = 0;

    private float capacityMultiplier = 1.0f;
    private float regenMultiplier = 1.0f;
    private int flatCapacityBonus = 0;
    private boolean connectionBlocked = false;
    private boolean permanentJam = false;

    // --- СЕТЕРИ (Повернено все для EnergyCommand та інших) ---
    public void setEnergy(int amount) { this.energy = Math.max(0, amount); }
    public void setLevel(int level) { this.level = Math.min(Math.max(1, level), TacionianConfig.MAX_LEVEL.get()); }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    public void setFractionalExperience(float f) { this.fractionalExperience = f; }
    public void setPermanentJam(boolean enabled) { this.permanentJam = enabled; }
    public void setCustomColor(int color) { this.customColor = color; }

    // --- ГЕТЕРИ (Повернено getEnergyPercent для StabilizationPlateBlock) ---
    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getCustomColor() { return customColor; }
    public int getEnergyPercent() { return (int)(getEnergyFraction() * 100); }

    public int getMaxEnergy() {
        int base = 1000 + (level - 1) * TacionianConfig.ENERGY_PER_LEVEL.get();
        return (int) ((base + flatCapacityBonus) * capacityMultiplier);
    }

    public int getRequiredExp() { return level < 20 ? 500 + (level * 100) : 2500 + (level * 250); }
    public int getRegenRate() { return (int) (TacionianConfig.BASE_REGEN.get() * regenMultiplier + (level * 2.0)); }
    public float getEnergyFraction() { return getMaxEnergy() > 0 ? (float) energy / getMaxEnergy() : 0; }

    // --- СТАН (Статуси) ---
    public boolean isOverloaded() {float threshold = (this.level <= 5) ? 0.95f : 0.8f;
        return getEnergyFraction() >= threshold;}
    public boolean isCriticalOverload() { return getEnergyFraction() >= 0.95f; }
    public boolean isCriticalLow() { return getEnergyFraction() < 0.05f; }
    public boolean isConnectionBlocked() { return connectionBlocked || permanentJam; }

    // --- ЛОГІКА ЕНЕРГІЇ (Повернено extractEnergyWithExp для зарядних пристроїв) ---
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

    public int extractEnergyWithExp(int amount, boolean simulate, ServerPlayer player) {
        int toExt = Math.min(amount, this.energy);
        if (!simulate && toExt > 0) {
            this.energy -= toExt;
            addExperience(toExt * TacionianConfig.EXP_MULTIPLIER.get().floatValue(), player);
        }
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

    // --- ТІК ТА СИНХРОНІЗАЦІЯ ---
    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        this.capacityMultiplier = 1.0f;
        this.regenMultiplier = 1.0f;
        this.flatCapacityBonus = 0;
        this.connectionBlocked = false;
        this.customColor = -1;

        MinecraftForge.EVENT_BUS.post(new TachyonEnergyUpdateEvent(player, this));

        if (stabilizedTimer > 0) stabilizedTimer--;
        if (remoteStabilizedTimer > 0) remoteStabilizedTimer--;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;

        if (getEnergy() >= getMaxEnergy()) {
            if (!isStabilized() && !isRemoteStabilized()) {
                player.level().explode(null, player.getX(), player.getY(), player.getZ(), 5.0f, true, Level.ExplosionInteraction.BLOCK);
                player.hurt(player.damageSources().generic(), Float.MAX_VALUE);
                this.energy = 0;
                this.sync(serverPlayer);
                return;
            }
        }

        if (!permanentJam && !isRemoteNoDrain() && player.tickCount % 20 == 0) {
            int regenTarget = (this.level < 5) ? (int)(getMaxEnergy() * 0.9f) : getMaxEnergy();
            if (this.energy < regenTarget) {
                receiveEnergyPure(getRegenRate(), false);
                this.sync(serverPlayer);
            }
        }

        PlayerEnergyEffects.apply(serverPlayer, this);
    }

    // --- ТАЙМЕРИ ТА NBT ---
    public void setStabilized(boolean v) { if (v) this.stabilizedTimer = 20; }
    public boolean isStabilized() { return stabilizedTimer > 0; }
    public void setRemoteStabilized(boolean v) { if (v) this.remoteStabilizedTimer = 20; }
    public boolean isRemoteStabilized() { return remoteStabilizedTimer > 0; }
    public void setRemoteNoDrain(boolean v) { if (v) this.remoteNoDrainTimer = 20; }
    public boolean isRemoteNoDrain() { return remoteNoDrainTimer > 0; }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy); nbt.putInt("level", level);
        nbt.putInt("exp", experience); nbt.putBoolean("jammed", permanentJam);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("exp");
        this.permanentJam = nbt.getBoolean("jammed");
    }

    public static class TachyonEnergyUpdateEvent extends Event {
        public final Player player; public final PlayerEnergy energy;
        public TachyonEnergyUpdateEvent(Player p, PlayerEnergy e) { this.player = p; this.energy = e; }
    }
}