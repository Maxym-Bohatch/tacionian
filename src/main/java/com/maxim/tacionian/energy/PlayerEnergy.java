package com.maxim.tacionian.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;
    private int stabilizationThreshold = 100; // 100 = Off

    private boolean stabilized, remoteStabilized, remoteNoDrain;
    private int energyDrainedThisTick = 0;
    private long lastTickTime = 0;

    public void tick(Player player) {
        if (this.level < 1) this.level = 1;

        // ПРИМУСОВА СТАБІЛІЗАЦІЯ (якщо активний режим)
        if ((this.stabilized || this.remoteStabilized) && stabilizationThreshold < 100) {
            int limit = (int) (getMaxEnergy() * (stabilizationThreshold / 100.0f));
            if (this.energy > limit) {
                this.energy = limit;
            }
        }

        int max = getMaxEnergy();
        int regen = getRegenRate();

        // Регенерація
        if (level <= 5) {
            if (this.energy < (int)(max * 0.95)) receiveEnergy(regen, false);
        } else {
            if (this.energy < max) receiveEnergy(regen, false);
        }

        // Штрафи
        if (player.tickCount % 20 == 0) {
            if ((isOverloaded() || isCriticalLow()) && !isStabilized() && !isRemoteStabilized()) {
                removeExperience(5 + (level * 2), player);
            }
        }
    }

    public int getMaxEnergy() { return 1000 + (level - 1) * 500; }
    public int getRegenRate() { return 1 + (level / 3); }
    public int getRequiredExp() { return level * 500; } // Фіксовано 500
    public int getMaxDrainPerTick() { return 100 + (level * 20); }

    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getStabilizationThreshold() { return stabilizationThreshold; }
    public void setStabilizationThreshold(int value) { this.stabilizationThreshold = value; }

    public float getRatio() { return getMaxEnergy() > 0 ? (float) energy / getMaxEnergy() : 0; }
    public boolean isOverloaded() { return getRatio() > 0.97f; }
    public boolean isCriticalLow() { return getRatio() < 0.15f; }

    public void setEnergy(int amount) { this.energy = Mth.clamp(amount, 0, getMaxEnergy()); }

    public void receiveEnergy(int amount, boolean simulate) {
        int space = getMaxEnergy() - energy;
        int toAdd = Math.min(amount, space);
        if (!simulate) energy += toAdd;
    }

    public int extractEnergyWithExp(int amount, boolean simulate, long currentTick, Player player) {
        if (this.remoteNoDrain) return 0;
        int extracted = extractEnergyBase(amount, simulate, currentTick);
        if (!simulate && extracted > 0) addExperience(Math.max(1, extracted / 5), player);
        return extracted;
    }

    public int extractEnergyPure(int amount, boolean simulate, long currentTick) {
        return extractEnergyBase(amount, simulate, currentTick);
    }

    private int extractEnergyBase(int amount, boolean simulate, long currentTick) {
        if (currentTick != lastTickTime) { energyDrainedThisTick = 0; lastTickTime = currentTick; }
        int limit = getMaxDrainPerTick() - energyDrainedThisTick;
        int toExtract = Math.min(Math.min(amount, energy), limit);
        if (toExtract <= 0) return 0;
        if (!simulate) { energy -= toExtract; energyDrainedThisTick += toExtract; }
        return toExtract;
    }

    public void addExperience(int amount, Player player) {
        this.experience += amount;
        int oldLevel = this.level;
        while (this.experience >= getRequiredExp()) {
            this.experience -= getRequiredExp();
            this.level++;
        }
        if (this.level > oldLevel && player != null && !player.level().isClientSide) {
            player.sendSystemMessage(Component.translatable("message.tacionian.level_up", this.level));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
    }

    public void removeExperience(int amount, Player player) {
        this.experience -= amount;
        if (this.experience < 0) {
            if (this.level > 1) {
                this.level--;
                this.experience = (int)(getRequiredExp() * 0.9f);
                if (player != null && !player.level().isClientSide) {
                    player.sendSystemMessage(Component.translatable("message.tacionian.level_down", this.level).withStyle(net.minecraft.ChatFormatting.RED));
                }
            } else { this.experience = 0; }
        }
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("tacion_energy", energy);
        nbt.putInt("tacion_level", level);
        nbt.putInt("tacion_exp", experience);
        nbt.putInt("tacion_threshold", stabilizationThreshold);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("tacion_energy");
        this.level = Math.max(1, nbt.getInt("tacion_level"));
        this.experience = nbt.getInt("tacion_exp");
        this.stabilizationThreshold = nbt.contains("tacion_threshold") ? nbt.getInt("tacion_threshold") : 100;
    }

    public void setStabilized(boolean v) { this.stabilized = v; }
    public boolean isStabilized() { return stabilized; }
    public void setRemoteStabilized(boolean v) { this.remoteStabilized = v; }
    public boolean isRemoteStabilized() { return remoteStabilized; }
    public void setRemoteNoDrain(boolean v) { this.remoteNoDrain = v; }
    public boolean isRemoteNoDrain() { return remoteNoDrain; }
}