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
    private float fractionalExperience = 0.0f;

    private boolean stabilized = false;
    private boolean remoteStabilized = false;
    private boolean remoteNoDrain = false;

    // Таймери для запобігання мерехтіння HUD
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

        // Оновлення логічних станів на основі таймерів
        this.stabilized = (stabilizedTimer > 0);
        this.remoteStabilized = (remoteStabilizedTimer > 0);
        this.remoteNoDrain = (remoteNoDrainTimer > 0);

        // Зменшення таймерів
        if (stabilizedTimer > 0) stabilizedTimer--;
        if (remoteStabilizedTimer > 0) remoteStabilizedTimer--;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;

        // Пасивна безпека для новачків (рівень 5 і нижче)
        if (this.level <= 5 && !remoteStabilized && !remoteNoDrain) {
            int safeLimit = (int) (getMaxEnergy() * 0.95f);
            if (this.energy > safeLimit) this.energy = safeLimit;
        }

        // Регенерація енергії
        if (!remoteNoDrain && player.tickCount % 20 == 0) {
            int regenMax = (level <= 5) ? (int)(getMaxEnergy() * 0.95f) : getMaxEnergy();
            if (this.energy < regenMax) {
                receiveEnergy(getRegenRate(), false);
                this.sync(serverPlayer);
            }
        }

        // Штраф до досвіду при перевантаженні
        if (isOverloaded()) {
            float penalty = Math.max(0.1f, (this.energy - getMaxEnergy()) / 100.0f);
            decreaseExperience(penalty, serverPlayer);
        }

        // Штраф при критично низькій енергії
        if (isCriticalLow() && this.level > 1 && !remoteNoDrain) {
            if (player.tickCount % 40 == 0) {
                decreaseExperience(1.0f, serverPlayer);
            }
        }
    }

    // Сетери з таймерами для зовнішніх блоків/предметів
    public void setStabilized(boolean v) {
        if (v) this.stabilizedTimer = 15; // Трохи збільшено для надійності
    }

    public void setRemoteStabilized(boolean v) {
        if (v) this.remoteStabilizedTimer = 15;
    }

    public void setRemoteNoDrain(boolean v) {
        if (v) this.remoteNoDrainTimer = 15;
    }

    // Система досвіду та рівнів
    public void addExperience(float amount, ServerPlayer player) {
        if (amount <= 0) return;
        this.fractionalExperience += amount;

        if (this.fractionalExperience >= 1.0f) {
            int wholeExp = (int) this.fractionalExperience;
            this.experience += wholeExp;
            this.fractionalExperience -= wholeExp;

            while (this.experience >= getRequiredExp()) {
                this.experience -= getRequiredExp();
                this.level++;
                if (player != null) {
                    // Використовуємо локалізацію для повідомлення про рівень
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
                // Повертаємо 75% досвіду попереднього рівня, щоб не падати в нуль
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

    // Базові сетери (використовуються в командах)
    public void setEnergy(int energy) { this.energy = Math.max(0, energy); }
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }

    // Геттери та розрахунки
    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getMaxEnergy() { return 1000 + (level - 1) * 500; }
    public int getRequiredExp() { return 500 + (level * 100); }
    public int getRegenRate() { return 1 + (level / 3); }
    public int getEnergyPercent() { return (int)((float)energy / getMaxEnergy() * 100); }

    public boolean isOverloaded() { return this.energy > getMaxEnergy(); }
    public boolean isCriticalLow() { return getEnergyPercent() < 5; }
    public boolean isStabilized() { return stabilized; }
    public boolean isRemoteStabilized() { return remoteStabilized; }
    public boolean isRemoteNoDrain() { return remoteNoDrain; }

    // Методи взаємодії з енергією
    public void receiveEnergy(int amount, boolean simulate) {
        if (!simulate) energy += amount;
    }

    public int extractEnergyWithExp(int amount, boolean simulate, ServerPlayer player) {
        int toExt = Math.min(amount, energy);
        if (!simulate && toExt > 0) {
            energy -= toExt;
            addExperience(toExt * 0.5f, player);
        }
        return toExt;
    }

    public int extractEnergyPure(int amount, boolean simulate) {
        int toExt = Math.min(amount, this.energy);
        if (!simulate) this.energy -= toExt;
        return toExt;
    }

    // Збереження даних (NBT)
    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("level", level);
        nbt.putInt("exp", experience);
        nbt.putFloat("fractionalExp", fractionalExperience);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("exp");
        this.fractionalExperience = nbt.getFloat("fractionalExp");
    }
}