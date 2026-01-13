package com.maxim.tacionian.energy;

import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;

    private boolean stabilized, remoteStabilized, remoteNoDrain;

    /** ВІДПРАВКА ДАНИХ КЛІЄНТУ (HUD) */
    public void sync(ServerPlayer player) {
        if (player != null) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        // Логіка пасивної безпеки для початківців (до 5 рівня енергія не перевищує 95%)
        if (this.level <= 5) {
            int safeLimit = (int) (getMaxEnergy() * 0.95f);
            if (this.energy > safeLimit) this.energy = safeLimit;
        }

        // Пасивна регенерація
        if (!remoteNoDrain) {
            int regenMax = (level <= 5) ? (int)(getMaxEnergy() * 0.95f) : getMaxEnergy();
            if (this.energy < regenMax) receiveEnergy(getRegenRate(), false);
        }
    }

    // --- СТАТУСИ ТА РОЗРАХУНКИ ---

    /** Повертає поточний відсоток заряду від 0 до 100 */
    public int getEnergyPercent() {
        if (getMaxEnergy() <= 0) return 0;
        return (int)((float)energy / getMaxEnergy() * 100);
    }

    public int getMaxEnergy() {
        return 1000 + (level - 1) * 500;
    }

    public int getRegenRate() {
        return 1 + (level / 3);
    }

    public int getRequiredExp() {
        return 500 + (level * 100);
    }

    public int getStabilityThreshold() {
        return 10;
    }

    public boolean isOverloaded() {
        return getEnergyPercent() > 95;
    }

    public boolean isCriticalLow() {
        return getEnergyPercent() < 5;
    }

    public boolean isStabilized() { return stabilized; }
    public boolean isRemoteStabilized() { return remoteStabilized; }
    public boolean isRemoteNoDrain() { return remoteNoDrain; }

    public void setStabilized(boolean v) { this.stabilized = v; }
    public void setRemoteStabilized(boolean v) { this.remoteStabilized = v; }
    public void setRemoteNoDrain(boolean v) { this.remoteNoDrain = v; }

    // --- МАНІПУЛЯЦІЇ ЕНЕРГІЄЮ ---
    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, getMaxEnergy()));
    }

    public void receiveEnergy(int amount, boolean simulate) {
        int toAdd = Math.min(amount, getMaxEnergy() - energy);
        if (!simulate) energy += toAdd;
    }

    /** Витрата енергії з отриманням досвіду */
    public int extractEnergyWithExp(int amount, boolean simulate, ServerPlayer player) {
        int toExt = Math.min(amount, energy);
        if (!simulate && toExt > 0) {
            energy -= toExt;
            addExperience(toExt / 2, player);
        }
        return toExt;
    }

    /** Чисте вилучення енергії без досвіду */
    public int extractEnergyPure(int amount, boolean simulate) {
        int toExt = Math.min(amount, this.energy);
        if (!simulate) {
            this.energy -= toExt;
        }
        return toExt;
    }

    // --- ЛОГІКА ДОСВІДУ ТА РІВНІВ ---
    public void addExperience(int amount, ServerPlayer player) {
        this.experience += amount;

        boolean leveledUp = false;
        while (this.experience >= getRequiredExp()) {
            this.experience -= getRequiredExp();
            this.level++;
            leveledUp = true;
        }

        if (leveledUp && player != null) {
            player.sendSystemMessage(Component.literal("§b[Tacionian] §fВаш рівень ядра підвищено до: §6" + this.level));
            this.sync(player);
        }
    }

    // --- ЗБЕРЕЖЕННЯ ДАНИХ ---
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

    // Геттери
    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
}