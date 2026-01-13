package com.maxim.tacionian.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;

    private boolean stabilized, remoteStabilized, remoteNoDrain;

    public void tick(Player player) {
        // Логіка пасивної безпеки для початківців
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

    // --- МЕТОДИ ДЛЯ ПЕРЕВІРКИ СТАНУ (Використовуються в Effects та Resolver) ---

    /** Перевантаження: більше 95% */
    public boolean isOverloaded() {
        return getEnergyPercent() > 95;
    }

    /** Критично низький заряд: менше 5% */
    public boolean isCriticalLow() {
        return getEnergyPercent() < 5;
    }

    public boolean isStabilized() { return stabilized; }
    public boolean isRemoteStabilized() { return remoteStabilized; }

    // --- СЕТТЕРИ СТАТУСІВ ---
    public void setStabilized(boolean v) { this.stabilized = v; }
    public void setRemoteStabilized(boolean v) { this.remoteStabilized = v; }
    public void setRemoteNoDrain(boolean v) { this.remoteNoDrain = v; }

    // --- ОСНОВНІ РОЗРАХУНКИ ---
    public int getMaxEnergy() { return 1000 + (level - 1) * 500; }
    public int getRegenRate() { return 1 + (level / 3); }
    public int getRequiredExp() { return 500 + (level * 100); }

    /** Повертає відсоток заряду від 0 до 100 */
    public int getEnergyPercent() {
        if (getMaxEnergy() <= 0) return 0;
        return (int)((float)energy / getMaxEnergy() * 100);
    }

    // --- МАНІПУЛЯЦІЇ ЕНЕРГІЄЮ ---
    public int extractEnergyWithExp(int amount, boolean simulate, Player player) {
        int toExt = Math.min(amount, energy);
        if (!simulate && toExt > 0) {
            energy -= toExt;
            addExperience(toExt / 2, player);
        }
        return toExt;
    }

    public int extractEnergyPure(int amount, boolean simulate) {
        int toExt = Math.min(amount, energy);
        if (!simulate) energy -= toExt;
        return toExt;
    }

    public void receiveEnergy(int amount, boolean simulate) {
        int toAdd = Math.min(amount, getMaxEnergy() - energy);
        if (!simulate) energy += toAdd;
    }

    // --- ДОСВІД ТА РІВНІ ---
    public void addExperience(int amount, Player player) {
        this.experience += amount;
        while (this.experience >= getRequiredExp()) {
            this.experience -= getRequiredExp();
            this.level++;
            if (player != null && !player.level().isClientSide) {
                player.sendSystemMessage(Component.translatable("message.tacionian.level_up", level));
            }
        }
    }

    // --- ГЕТТЕРИ ТА СЕТТЕРИ ДАНИХ ---
    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public void setEnergy(int energy) { this.energy = energy; }

    // --- ЗБЕРЕЖЕННЯ ---
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