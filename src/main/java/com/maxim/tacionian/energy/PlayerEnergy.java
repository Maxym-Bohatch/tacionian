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

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;
    private float fractionalExperience = 0.0f;

    private int stabilizedTimer = 0;
    private int remoteStabilizedTimer = 0;
    private int remoteNoDrainTimer = 0;

    private float capacityMultiplier = 1.0f;
    private float regenMultiplier = 1.0f;
    private int flatCapacityBonus = 0;
    private float drainModifier = 0.0f;

    private boolean connectionBlocked = false;
    private boolean permanentJam = false;

    // --- Мережева синхронізація ---
    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // Скидання динамічних множників щоразу (щоб ефекти могли їх накласти знову)
        this.capacityMultiplier = 1.0f;
        this.regenMultiplier = 1.0f;
        this.flatCapacityBonus = 0;
        this.drainModifier = 0.0f;
        this.connectionBlocked = false;

        // Публікуємо івент для аддонів
        MinecraftForge.EVENT_BUS.post(new TachyonEnergyUpdateEvent(player, this));

        // Оновлення таймерів стабілізації
        if (stabilizedTimer > 0) stabilizedTimer--;
        if (remoteStabilizedTimer > 0) remoteStabilizedTimer--;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;

        // Логіка пасивної регенерації
        if (!permanentJam) {
            if (!isRemoteNoDrain() && player.tickCount % 20 == 0) {
                int regenTarget = (this.level < 5) ? (int)(getMaxEnergy() * 0.9f) : getMaxEnergy();
                if (this.energy < regenTarget) {
                    receiveEnergyPure(getRegenRate(), false);
                    this.sync(serverPlayer);
                }
            }
        }

        // Застосування ефектів (вогонь, сповільнення, вибух)
        PlayerEnergyEffects.apply(serverPlayer, this);
    }

    // --- СТАБІЛІЗАЦІЯ ---
    public void setStabilized(boolean v) { if (v) this.stabilizedTimer = 20; }
    public boolean isStabilized() { return stabilizedTimer > 0; }
    public void setRemoteStabilized(boolean v) { if (v) this.remoteStabilizedTimer = 20; }
    public boolean isRemoteStabilized() { return remoteStabilizedTimer > 0; }
    public void setRemoteNoDrain(boolean v) { if (v) this.remoteNoDrainTimer = 20; }
    public boolean isRemoteNoDrain() { return remoteNoDrainTimer > 0; }

    // --- СТАН ТА ЕФЕКТИ ---
    public boolean isDeadlyOverloadEnabled() { return true; }
    public boolean isCriticalLow() { return energy < (getMaxEnergy() * 0.05f); }
    public boolean isOverloaded() { return this.energy > getMaxEnergy(); }
    public int getEnergyPercent() { return (int)((float)energy / getMaxEnergy() * 100); }
    public float getEnergyFraction() { return (float)energy / getMaxEnergy(); }

    // --- УПРАВЛІННЯ (СЕТТЕРИ) ---
    public void setConnectionBlocked(boolean v) { this.connectionBlocked = v; }
    public void setPermanentJam(boolean v) { this.permanentJam = v; }
    public boolean isConnectionBlocked() { return connectionBlocked || permanentJam; }

    public void setEnergy(int amount) { this.energy = Math.max(0, amount); }
    public void setLevel(int level) {
        this.level = Math.min(Math.max(1, level), TacionianConfig.MAX_LEVEL.get());
    }
    public void setExperience(int exp) {
        this.experience = Math.max(0, exp);
        this.fractionalExperience = 0.0f;
    }

    // --- МАТЕМАТИКА ЕНЕРГІЇ ТА ДОСВІДУ ---
    public int receiveEnergyPure(int amount, boolean simulate) {
        int space = Math.max(0, getMaxEnergy() * 2 - this.energy);
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
            float multiplier = TacionianConfig.EXP_MULTIPLIER.get().floatValue();
            addExperience(toExt * multiplier, player);
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

    // --- ГЕТТЕРИ ТА ПАРАМЕТРИ ---
    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getMaxEnergy() {
        int base = 1000 + (level - 1) * TacionianConfig.ENERGY_PER_LEVEL.get();
        return (int) ((base + flatCapacityBonus) * capacityMultiplier);
    }
    public int getRequiredExp() { return level < 20 ? 500 + (level * 100) : 2500 + (level * 250); }
    public int getRegenRate() { return (int) (TacionianConfig.BASE_REGEN.get() * regenMultiplier + (level * 2.0)); }
    public enum EnergyState {
        DEADLY_OVERLOAD(100), // Найвищий пріоритет
        JAMMED(90),
        OVERLOADED(80),
        CRITICAL_LOW(70),
        REMOTE_TRANSFER(60),
        STABILIZED(50),
        REMOTE_STABILIZED(40),
        NORMAL(0);

        public final int priority;
        EnergyState(int p) { this.priority = p; }
    }

    public EnergyState getCurrentState() {
        if (getEnergyPercent() > 150) return EnergyState.DEADLY_OVERLOAD;
        if (isConnectionBlocked()) return EnergyState.JAMMED;
        if (isOverloaded()) return EnergyState.OVERLOADED;
        if (isCriticalLow()) return EnergyState.CRITICAL_LOW;
        if (isRemoteNoDrain()) return EnergyState.REMOTE_TRANSFER;
        if (isStabilized()) return EnergyState.STABILIZED;
        if (isRemoteStabilized()) return EnergyState.REMOTE_STABILIZED;
        return EnergyState.NORMAL;
    }

    // --- NBT ЗБЕРЕЖЕННЯ ---
    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("level", level);
        nbt.putInt("exp", experience);
        nbt.putBoolean("jammed", permanentJam);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("exp");
        this.permanentJam = nbt.getBoolean("jammed");
    }

    // --- Подія для аддонів ---
    public static class TachyonEnergyUpdateEvent extends Event {
        public final Player player;
        public final PlayerEnergy energy;
        public TachyonEnergyUpdateEvent(Player p, PlayerEnergy e) { this.player = p; this.energy = e; }
    }
}