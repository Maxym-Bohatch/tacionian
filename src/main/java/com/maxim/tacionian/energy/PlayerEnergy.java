package com.maxim.tacionian.energy;

import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;
    private float fractionalExperience = 0.0f;

    private boolean stabilized = false;
    private boolean remoteStabilized = false;
    private boolean remoteNoDrain = false;

    private int stabilizedTimer = 0;
    private int remoteStabilizedTimer = 0;
    private int remoteNoDrainTimer = 0;

    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        this.stabilized = (stabilizedTimer > 0);
        this.remoteStabilized = (remoteStabilizedTimer > 0);
        this.remoteNoDrain = (remoteNoDrainTimer > 0);

        if (stabilizedTimer > 0) stabilizedTimer--;
        if (remoteStabilizedTimer > 0) remoteStabilizedTimer--;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;

        // Пасивна безпека для новачків
        if (this.level <= 3 && !remoteStabilized && !remoteNoDrain) {
            int safeLimit = (int) (getMaxEnergy() * 0.95f);
            if (this.energy > safeLimit) this.energy = safeLimit;
        }

        // Регенерація
        if (!remoteNoDrain && player.tickCount % 20 == 0) {
            int regenTarget = getMaxEnergy();
            if (this.level < 5) regenTarget = (int)(getMaxEnergy() * 0.9f);

            if (this.energy < regenTarget) {
                int amount = getRegenRate();
                receiveEnergy(amount, false);
                if (amount > 0) this.sync(serverPlayer);
            }
        }

        if (isOverloaded()) {
            float penalty = Math.max(0.1f, (this.energy - getMaxEnergy()) / 100.0f);
            decreaseExperience(penalty, serverPlayer);
        }

        if (isCriticalLow() && this.level > 1 && !remoteNoDrain) {
            if (player.tickCount % 40 == 0) {
                decreaseExperience(1.0f, serverPlayer);
            }
        }
    }

    public void addExperience(float amount, ServerPlayer player) {
        // Використання ліміту з конфігу
        if (amount <= 0 || this.level >= TacionianConfig.MAX_LEVEL.get()) return;
        this.fractionalExperience += amount;

        if (this.fractionalExperience >= 1.0f) {
            int wholeExp = (int) this.fractionalExperience;
            this.experience += wholeExp;
            this.fractionalExperience -= wholeExp;

            while (this.experience >= getRequiredExp() && this.level < TacionianConfig.MAX_LEVEL.get()) {
                this.experience -= getRequiredExp();
                this.level++;
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.tacionian.level_up", this.level)
                            .withStyle(ChatFormatting.AQUA));
                }
            }
            if (player != null) this.sync(player);
        }
    }

    public void decreaseExperience(float amount, ServerPlayer player) {
        this.experience -= (int)amount;
        if (this.experience < 0) {
            if (this.level > 1) {
                this.level--;
                this.experience = (int) (getRequiredExp() * 0.75f);
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.tacionian.level_down", this.level)
                            .withStyle(ChatFormatting.DARK_RED));
                }
            } else {
                this.experience = 0;
            }
        }
        if (player != null && player.tickCount % 20 == 0) this.sync(player);
    }

    // Розрахунок максимуму через конфіг
    public int getMaxEnergy() {
        return 1000 + (level - 1) * TacionianConfig.ENERGY_PER_LEVEL.get();
    }

    public int getRequiredExp() {
        if (level < 20) return 500 + (level * 100);
        return 2500 + (level * 250);
    }

    // Регенерація через конфіг
    public int getRegenRate() {
        double scale = TacionianConfig.BASE_REGEN.get() + (level * 2.0) + (Math.pow(level, 2) / 200.0);
        return (int) scale;
    }

    public int extractEnergyWithExp(int amount, boolean simulate, ServerPlayer player) {
        int toExt = Math.min(amount, energy);
        if (!simulate && toExt > 0) {
            energy -= toExt;
            // Коефіцієнт досвіду з конфігу
            addExperience(toExt * TacionianConfig.EXP_MULTIPLIER.get().floatValue(), player);
        }
        return toExt;
    }

    // Решта методів...
    public void setStabilized(boolean v) { if (v) this.stabilizedTimer = 15; }
    public void setRemoteStabilized(boolean v) { if (v) this.remoteStabilizedTimer = 15; }
    public void setRemoteNoDrain(boolean v) { if (v) this.remoteNoDrainTimer = 15; }
    public void setEnergy(int energy) { this.energy = Math.max(0, energy); }
    public void setLevel(int level) { this.level = Math.min(Math.max(1, level), TacionianConfig.MAX_LEVEL.get()); }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getEnergyPercent() { return (int)((float)energy / getMaxEnergy() * 100); }
    public boolean isOverloaded() { return this.energy > getMaxEnergy(); }
    public boolean isCriticalLow() { return getEnergyPercent() < 5; }
    public boolean isStabilized() { return stabilized; }
    public boolean isRemoteStabilized() { return remoteStabilized; }
    public boolean isRemoteNoDrain() { return remoteNoDrain; }
    public void receiveEnergy(int amount, boolean simulate) { if (!simulate) energy += amount; }
    public int extractEnergyPure(int amount, boolean simulate) { int toExt = Math.min(amount, this.energy); if (!simulate) this.energy -= toExt; return toExt; }
    public void saveNBTData(CompoundTag nbt) { nbt.putInt("energy", energy); nbt.putInt("level", level); nbt.putInt("exp", experience); nbt.putFloat("fractionalExp", fractionalExperience); }
    public void loadNBTData(CompoundTag nbt) { this.energy = nbt.getInt("energy"); this.level = Math.min(Math.max(1, nbt.getInt("level")), TacionianConfig.MAX_LEVEL.get()); this.experience = nbt.getInt("exp"); this.fractionalExperience = nbt.getFloat("fractionalExp"); }
}