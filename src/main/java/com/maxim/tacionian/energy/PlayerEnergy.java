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

    // Статуси стабілізації (важливо для EnergyControlResolver)
    private boolean stabilized = false;
    private boolean remoteStabilized = false;
    private boolean remoteNoDrain = false;

    /** ВІДПРАВКА ДАНИХ КЛІЄНТУ (HUD) */
    public void sync(ServerPlayer player) {
        if (player != null) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // 1. Пасивна безпека для початківців (до 5 рівня)
        if (this.level <= 5) {
            int safeLimit = (int) (getMaxEnergy() * 0.95f);
            if (this.energy > safeLimit) this.energy = safeLimit;
        }

        // 2. Пасивна регенерація
        if (!remoteNoDrain && player.tickCount % 20 == 0) {
            int regenMax = (level <= 5) ? (int)(getMaxEnergy() * 0.95f) : getMaxEnergy();
            if (this.energy < regenMax) {
                receiveEnergy(getRegenRate(), false);
                // Синхронізуємо після регенерації
                this.sync(serverPlayer);
            }
        }

        // 3. Логіка штрафів за перевантаження (> 100%)
        if (this.energy > getMaxEnergy()) {
            // Штраф: 1 од. досвіду за кожні 10 од. зайвої енергії
            int overloadPenalty = Math.max(1, (this.energy - getMaxEnergy()) / 10);
            decreaseExperience(overloadPenalty, serverPlayer);

            if (player.getRandom().nextFloat() < 0.05f) {
                player.displayClientMessage(Component.translatable("message.tacionian.overload_critical").withStyle(ChatFormatting.RED), true);
            }
        }

        // 4. Штраф за критичний дефіцит (< 5%)
        if (isCriticalLow() && this.level > 1 && !remoteNoDrain) {
            decreaseExperience(2, serverPlayer);
        }
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

    public void decreaseExperience(int amount, ServerPlayer player) {
        this.experience -= amount;
        if (this.experience < 0) {
            if (this.level > 1) {
                this.level--;
                this.experience = (int) (getRequiredExp() * 0.75f); // Ставимо 75% досвіду на новому рівні
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.tacionian.level_down", this.level)
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
            } else {
                this.experience = 0;
            }
        }
        if (player != null && player.tickCount % 20 == 0) this.sync(player);
    }

    // --- ГЕТТЕРИ ТА СЕТТЕРИ СТАТУСІВ (Для Resolver) ---

    public boolean isStabilized() { return stabilized; }
    public void setStabilized(boolean v) { this.stabilized = v; }

    public boolean isRemoteStabilized() { return remoteStabilized; }
    public void setRemoteStabilized(boolean v) { this.remoteStabilized = v; }

    public boolean isRemoteNoDrain() { return remoteNoDrain; }
    public void setRemoteNoDrain(boolean v) { this.remoteNoDrain = v; }

    // --- РОЗРАХУНКИ ---

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
    public boolean isCriticalLow() {
        return getEnergyPercent() < 5;
    }

    public boolean isOverloaded() {
        return this.energy > getMaxEnergy();
    }

    // --- МАНІПУЛЯЦІЇ ЕНЕРГІЄЮ ---

    public void setEnergy(int energy) {
        this.energy = Math.max(0, energy);
    }

    public void receiveEnergy(int amount, boolean simulate) {
        if (!simulate) energy += amount;
    }

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

    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
}