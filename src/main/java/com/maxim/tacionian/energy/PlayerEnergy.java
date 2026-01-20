package com.maxim.tacionian.energy;

import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class PlayerEnergy {
    public static final int MAX_LEVEL = 100;
    public static final float EXP_MULTIPLIER = 0.5f;

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

        if (this.level <= 3 && !remoteStabilized && !remoteNoDrain) {
            int safeLimit = (int) (getMaxEnergy() * 0.95f);
            if (this.energy > safeLimit) this.energy = safeLimit;
        }

        // --- ПОКРАЩЕНА РЕГЕНЕРАЦІЯ ---
        if (!remoteNoDrain && player.tickCount % 20 == 0) {
            int regenTarget = getMaxEnergy();
            if (this.level < 5) regenTarget = (int)(getMaxEnergy() * 0.9f);

            if (this.energy < regenTarget) {
                // Тепер регенерація відчутна
                int amount = getRegenRate();
                receiveEnergy(amount, false);

                // Синхронізуємо тільки якщо відбулася зміна
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

    // ... (методи setStabilized, addExperience, decreaseExperience без змін)

    public void setStabilized(boolean v) { if (v) this.stabilizedTimer = 15; }
    public void setRemoteStabilized(boolean v) { if (v) this.remoteStabilizedTimer = 15; }
    public void setRemoteNoDrain(boolean v) { if (v) this.remoteNoDrainTimer = 15; }

    public void addExperience(float amount, ServerPlayer player) {
        if (amount <= 0 || this.level >= MAX_LEVEL) return;
        this.fractionalExperience += amount;
        if (this.fractionalExperience >= 1.0f) {
            int wholeExp = (int) this.fractionalExperience;
            this.experience += wholeExp;
            this.fractionalExperience -= wholeExp;
            while (this.experience >= getRequiredExp() && this.level < MAX_LEVEL) {
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

    public void setEnergy(int energy) { this.energy = Math.max(0, energy); }
    public void setLevel(int level) { this.level = Math.min(Math.max(1, level), MAX_LEVEL); }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }

    public int getMaxEnergy() { return 1000 + (level - 1) * 500; }

    public int getRequiredExp() {
        if (level < 20) return 500 + (level * 100);
        return 2500 + (level * 250);
    }

    // --- НОВА ФОРМУЛА РЕГЕНЕРАЦІЇ ---
    public int getRegenRate() {
        // База (5) + Прогресія від рівня.
        // На 100 рівні: 5 + (100 * 2) + (100^2 / 200) = 5 + 200 + 50 = 255 Tx/сек.
        // Це заповнить 50к енергії за ~3 хвилини, що адекватно.
        double scale = 5 + (level * 2.0) + (Math.pow(level, 2) / 200.0);
        return (int) scale;
    }

    public int getEnergyPercent() { return (int)((float)energy / getMaxEnergy() * 100); }
    public boolean isOverloaded() { return this.energy > getMaxEnergy(); }
    public boolean isCriticalLow() { return getEnergyPercent() < 5; }
    public boolean isStabilized() { return stabilized; }
    public boolean isRemoteStabilized() { return remoteStabilized; }
    public boolean isRemoteNoDrain() { return remoteNoDrain; }

    public void receiveEnergy(int amount, boolean simulate) {
        if (!simulate) energy += amount;
    }

    public int extractEnergyWithExp(int amount, boolean simulate, ServerPlayer player) {
        int toExt = Math.min(amount, energy);
        if (!simulate && toExt > 0) {
            energy -= toExt;
            addExperience(toExt * EXP_MULTIPLIER, player);
        }
        return toExt;
    }

    public int extractEnergyPure(int amount, boolean simulate) {
        int toExt = Math.min(amount, this.energy);
        if (!simulate) this.energy -= toExt;
        return toExt;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("level", level);
        nbt.putInt("exp", experience);
        nbt.putFloat("fractionalExp", fractionalExperience);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.min(Math.max(1, nbt.getInt("level")), MAX_LEVEL);
        this.experience = nbt.getInt("exp");
        this.fractionalExperience = nbt.getFloat("fractionalExp");
    }
}