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

    private boolean stabilized, remoteStabilized, remoteNoDrain;

    public void sync(ServerPlayer player) {
        if (player != null) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // --- ЛОГІКА ПАСИВНОЇ БЕЗПЕКИ ТА РЕГЕНЕРАЦІЇ ---
        if (this.level <= 5) {
            int safeLimit = (int) (getMaxEnergy() * 0.95f);
            if (this.energy > safeLimit) this.energy = safeLimit;
        }

        if (!remoteNoDrain) {
            int regenMax = (level <= 5) ? (int)(getMaxEnergy() * 0.95f) : getMaxEnergy();
            if (this.energy < regenMax) receiveEnergy(getRegenRate(), false);
        }

        // --- НОВА ЛОГІКА ШТРАФІВ (ДЕГРАДАЦІЯ) ---

        // 1. Штраф за перевантаження (> 100%)
        if (this.energy > getMaxEnergy()) {
            // Кожні 10 одиниць надлишку додають 1 одиницю втрати досвіду за тік
            int overloadPenalty = Math.max(1, (this.energy - getMaxEnergy()) / 10);
            decreaseExperience(overloadPenalty, serverPlayer);

            if (player.getRandom().nextFloat() < 0.05f) {
                player.displayClientMessage(Component.translatable("message.tacionian.overload_critical").withStyle(ChatFormatting.RED), true);
            }
        }

        // 2. Штраф за критичний дефіцит (< 5%)
        // Якщо енергії майже немає, структура ядра стає крихкою
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

    /** НОВИЙ МЕТОД: Зменшення досвіду та втрата рівня */
    public void decreaseExperience(int amount, ServerPlayer player) {
        this.experience -= amount;

        if (this.experience < 0) {
            if (this.level > 1) {
                this.level--;
                // При падінні рівня ставимо досвід на 75%, щоб не було постійного стрибка туди-сюди
                this.experience = (int) (getRequiredExp() * 0.75f);

                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.tacionian.level_down", this.level)
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
            } else {
                this.experience = 0;
            }
        }

        // Синхронізуємо зміни, щоб HUD оновився
        if (player != null && player.tickCount % 10 == 0) {
            this.sync(player);
        }
    }

    // --- РЕШТА МЕТОДІВ (БЕЗ ЗМІН) ---

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

    public boolean isCriticalLow() {
        return getEnergyPercent() < 5;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, energy); // Дозволяємо тимчасове перевантаження
    }

    public void receiveEnergy(int amount, boolean simulate) {
        // Дозволяємо отримувати енергію навіть понад ліміт, але tick() почне штрафувати
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
    public void setRemoteStabilized(boolean v) { this.remoteStabilized = v; }
    public void setRemoteNoDrain(boolean v) { this.remoteNoDrain = v; }
}