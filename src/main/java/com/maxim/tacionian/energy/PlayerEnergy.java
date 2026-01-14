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
    private int energy = 0;
    private int level = 1;
    private int experience = 0;

    private boolean stabilized = false;
    private boolean remoteStabilized = false;
    private boolean remoteNoDrain = false;

    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // Пасивна безпека для новачків
        if (this.level <= 5) {
            int safeLimit = (int) (getMaxEnergy() * 0.95f);
            if (this.energy > safeLimit) this.energy = safeLimit;
        }

        // Регенерація
        if (!remoteNoDrain && player.tickCount % 20 == 0) {
            int regenMax = (level <= 5) ? (int)(getMaxEnergy() * 0.95f) : getMaxEnergy();
            if (this.energy < regenMax) {
                receiveEnergy(getRegenRate(), false);
            }
        }

        // Штраф за перевантаження
        if (isOverloaded()) {
            int penalty = Math.max(1, (this.energy - getMaxEnergy()) / 10);
            decreaseExperience(penalty, serverPlayer);
        }

        // Штраф за дефіцит
        if (isCriticalLow() && this.level > 1 && !remoteNoDrain) {
            decreaseExperience(2, serverPlayer);
        }
    }

    // Методи керування досвідом
    public void addExperience(int amount, ServerPlayer player) {
        this.experience += amount;
        while (this.experience >= getRequiredExp()) {
            this.experience -= getRequiredExp();
            this.level++;
            if (player != null) {
                player.sendSystemMessage(Component.literal("§b[Tacionian] §fРівень підвищено: §6" + this.level));
            }
        }
        if (player != null) this.sync(player);
    }

    public void decreaseExperience(int amount, ServerPlayer player) {
        this.experience -= amount;
        if (this.experience < 0) {
            if (this.level > 1) {
                this.level--;
                this.experience = (int) (getRequiredExp() * 0.75f);
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.tacionian.level_down", this.level).withStyle(ChatFormatting.DARK_RED));
                }
            } else {
                this.experience = 0;
            }
        }
    }

    // Геттери та Сеттери
    public void setEnergy(int energy) { this.energy = Math.max(0, energy); }
    public void setLevel(int level) { this.level = level; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getMaxEnergy() { return 1000 + (level - 1) * 500; }
    public int getRequiredExp() { return 500 + (level * 100); }
    public int getRegenRate() { return 1 + (level / 3); }
    public int getEnergyPercent() { return (int)((float)energy / getMaxEnergy() * 100); }
    public boolean isOverloaded() { return this.energy > getMaxEnergy(); }
    public boolean isCriticalLow() { return getEnergyPercent() < 5; }
    public int getStabilityThreshold() { return 10; }

    public void setStabilized(boolean v) { this.stabilized = v; }
    public boolean isStabilized() { return stabilized; }
    public void setRemoteStabilized(boolean v) { this.remoteStabilized = v; }
    public boolean isRemoteStabilized() { return remoteStabilized; }
    public void setRemoteNoDrain(boolean v) { this.remoteNoDrain = v; }
    public boolean isRemoteNoDrain() { return remoteNoDrain; }

    public void receiveEnergy(int amount, boolean simulate) { if (!simulate) energy += amount; }

    public int extractEnergyWithExp(int amount, boolean simulate, ServerPlayer player) {
        int toExt = Math.min(amount, energy);
        if (!simulate && toExt > 0) {
            energy -= toExt;
            addExperience(toExt / 2, player);
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
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("exp");
    }
}