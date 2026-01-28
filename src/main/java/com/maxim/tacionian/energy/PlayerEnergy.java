/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.energy;

import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.register.ModDamageSources;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;
    private float fractionalExperience = 0.0f;

    private boolean disconnected = false;
    private boolean physicsDisabled = false;
    private boolean pushbackEnabled = true;
    private boolean regenBlocked = false;
    private boolean remoteAccessBlocked = false;
    private boolean globalStabilized = false;

    private int stabilizedTimer = 0;
    private int interfaceStabilizedTimer = 0;
    private int plateStabilizedTimer = 0;
    private int remoteNoDrainTimer = 0;

    // --- ГЕТТЕРИ ТА СЕТТЕРИ ---
    public int getEnergy() { return energy; }
    public void setEnergy(int amount) { this.energy = Math.max(0, amount); }
    public int getLevel() { return level; }
    public void setLevel(int l) { this.level = Math.min(Math.max(1, l), TacionianConfig.MAX_LEVEL.get()); }
    public int getMaxEnergy() { return 1000 + (level - 1) * TacionianConfig.ENERGY_PER_LEVEL.get(); }
    public int getExperience() { return experience; }
    public void setExperience(int exp) { this.experience = exp; }
    public int getRequiredExp() { return 5000 + (level * 1500); }

    public int receiveEnergyPure(int amount, boolean simulate) {
        return receiveEnergyPure(amount, simulate, false);
    }

    public float getEnergyPercent() {
        return getEnergyFraction() * 100f;
    }

    public int getStabilizedTimer() {
        return stabilizedTimer;
    }
    public boolean isDisconnected() { return disconnected; }
    public void setDisconnected(boolean state) { this.disconnected = state; }
    public boolean isPhysicsDisabled() { return physicsDisabled; }
    public void setPhysicsDisabled(boolean state) { this.physicsDisabled = state; }
    public boolean isRegenBlocked() { return regenBlocked; }
    public void setRegenBlocked(boolean state) { this.regenBlocked = state; }
    public boolean isPushbackEnabled() { return pushbackEnabled; }
    public void setPushbackEnabled(boolean state) { this.pushbackEnabled = state; }
    public boolean isRemoteAccessBlocked() { return remoteAccessBlocked; }
    public void setRemoteAccessBlocked(boolean state) { this.remoteAccessBlocked = state; }

    public void setStabilized(boolean v) { if (v) stabilizedTimer = 30; }
    public boolean isStabilized() { return stabilizedTimer > 0; }
    public void setInterfaceStabilized(boolean v) { if (v) interfaceStabilizedTimer = 25; }
    public boolean isInterfaceStabilized() { return interfaceStabilizedTimer > 0; }
    public void setPlateStabilized(boolean v) { if (v) plateStabilizedTimer = 25; }
    public boolean isPlateStabilized() { return plateStabilizedTimer > 0; }
    public void setRemoteNoDrain(boolean v) { if (v) remoteNoDrainTimer = 30; }
    public boolean isRemoteNoDrain() { return remoteNoDrainTimer > 0; }

    public float getEnergyFraction() { return getMaxEnergy() > 0 ? (float) energy / (float) getMaxEnergy() : 0.0f; }

    public boolean isStabilizedLogicActive() {
        return (this.level <= 5) || isStabilized() || isInterfaceStabilized() || isPlateStabilized() || isRemoteNoDrain();
    }

    // --- СИСТЕМА ДОСВІДУ ---
    public void addExperience(float amount, ServerPlayer player) {
        if (player == null || disconnected) return;
        this.fractionalExperience += amount;
        if (this.fractionalExperience >= 1.0f) {
            int toAdd = (int) this.fractionalExperience;
            this.experience += toAdd;
            this.fractionalExperience -= toAdd;

            while (this.experience >= getRequiredExp() && level < TacionianConfig.MAX_LEVEL.get()) {
                this.experience -= getRequiredExp();
                this.level++;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MODE_SWITCH.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
                player.displayClientMessage(Component.translatable("message.tacionian.level_up", level), true);
            }
            sync(player);
        }
    }

    // --- ЕНЕРГІЯ ---
    public int receiveEnergyPure(int amount, boolean simulate, boolean ignoreLimits) {
        if (disconnected && !ignoreLimits) return 0;
        int max = getMaxEnergy();
        float limitMult;

        if (ignoreLimits) {
            limitMult = 2.0f;
        } else if (this.level <= 5) {
            limitMult = 0.8f; // Рівно 80% для новачків
        } else if (this.globalStabilized) {
            limitMult = 2.0f; // Стабілізатор (Mode 3)
        } else {
            limitMult = 0.95f; // Стандартний поріг
        }

        int cap = (int)(max * limitMult);
        if (this.energy >= cap) return 0;
        int added = Math.min(amount, cap - this.energy);
        if (!simulate) this.energy += added;
        return added;
    }

    public int extractEnergyPure(int amount, boolean simulate) {
        int extracted = Math.min(amount, this.energy);
        if (!simulate) this.energy -= extracted;
        return extracted;
    }

    // --- ТІК ---
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer sp = (ServerPlayer) player;
        ServerLevel world = (ServerLevel) player.level();

        if (stabilizedTimer > 0) stabilizedTimer--;
        if (interfaceStabilizedTimer > 0) interfaceStabilizedTimer--;
        if (plateStabilizedTimer > 0) plateStabilizedTimer--;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;

        float ratio = getEnergyFraction();
        this.globalStabilized = isStabilizedLogicActive() || hasUnrestrictedItem(player);

        // Попередження про низьку енергію
        if (ratio < 0.05f && sp.tickCount % 600 == 0 && !sp.isCreative()) {
            sp.displayClientMessage(Component.translatable("message.tacionian.low_energy_warning"), true);
        }

        // Візуальні ефекти перевантаження
        if (ratio > 0.85f && !player.isCreative()) {
            if (player.getRandom().nextFloat() < (ratio * 0.15f)) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
            }
            if (player.tickCount % 20 == 0) {
                float vol = Math.min(1.0f, (ratio - 0.8f) * 2);
                world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, vol, 0.5f + ratio);
            }
        }

        if (disconnected) {
            if (player.tickCount % 20 == 0) sync(sp);
            return;
        }

        // Фізика (Відштовхування)
        if (ratio > 0.9f && pushbackEnabled && !player.isCreative()) {
            handlePhysics(player, ratio, world);
        }

        // Колапс (Вибух)
        float explosionThreshold = globalStabilized ? 2.01f : 1.05f;
        if (ratio > explosionThreshold && !player.isCreative()) {
            doCollapse(sp, world, ratio);
            return;
        }

        handlePassiveLogic(sp, ratio);
        if (player.tickCount % 20 == 0) sync(sp);
    }

    private void handlePhysics(Player player, float ratio, ServerLevel world) {
        float startAt = globalStabilized ? 1.70f : 0.90f;
        if (ratio > startAt) {
            double p = ratio - startAt;
            player.fallDistance = 0;
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, (Math.sin(player.tickCount * 0.15) * 0.04) + (0.07 * p), movement.z);
            player.hurtMarked = true;

            List<Entity> nearby = world.getEntities(player, player.getBoundingBox().inflate(3.5 * ratio));
            for (Entity e : nearby) {
                if (e instanceof LivingEntity || e instanceof Projectile || e instanceof ItemEntity) {
                    Vec3 dir = e.position().subtract(player.position()).normalize().scale(p * 1.5);
                    e.push(dir.x, 0.15, dir.z);
                    if (world.random.nextFloat() < 0.05f)
                        world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4f, 2.0f);
                }
            }
        }
    }

    private void handlePassiveLogic(ServerPlayer sp, float ratio) {
        float erosionLimit = globalStabilized ? 1.80f : 0.80f;

        // Повідомлення про критичне перевантаження
        if (ratio > 1.50f && sp.tickCount % 100 == 0) {
            sp.displayClientMessage(Component.translatable("message.tacionian.overload_critical"), true);
        }

        if (ratio > erosionLimit && !sp.isCreative()) {
            this.experience -= (int)((ratio - erosionLimit) * 80);
        } else if (ratio < 0.01f && sp.tickCount % 80 == 0 && !sp.isCreative()) {
            this.experience -= 30;
            // Використовуємо повідомлення про ліміт безпеки при нулі
            sp.displayClientMessage(Component.translatable("message.tacionian.safety_limit"), true);
        }

        if (this.experience < 0) {
            if (this.level > 1) {
                this.level--;
                this.experience = getRequiredExp() / 2;
                sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1f);
                sp.displayClientMessage(Component.translatable("message.tacionian.level_down", level), true);
            } else this.experience = 0;
        }

        float regenCap = globalStabilized ? 2.0f : 0.95f;
        if (sp.tickCount % 20 == 0 && !regenBlocked && ratio < regenCap) {
            receiveEnergyPure(level + (energy / 50), false, false);
        }
    }

    private void doCollapse(ServerPlayer sp, ServerLevel world, float ratio) {
        world.sendParticles(ParticleTypes.FLASH, sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0f, 0.5f);
        world.explode(null, sp.getX(), sp.getY(), sp.getZ(), 4.5f * ratio, true, Level.ExplosionInteraction.BLOCK);

        // Використовуємо наш кастомний тип пошкодження
        sp.hurt(ModDamageSources.getTachyonDamage(world), Float.MAX_VALUE);

        this.level = Math.max(1, level - 3);
        this.energy = 0;
        this.experience = 0;
        sync(sp);
    }

    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("level", level);
        nbt.putInt("experience", experience);
        nbt.putBoolean("disconnected", disconnected);
        nbt.putBoolean("regenBlocked", regenBlocked);
        nbt.putBoolean("remoteBlocked", remoteAccessBlocked);
        nbt.putBoolean("pushbackEnabled", pushbackEnabled);
        nbt.putInt("t_stab", stabilizedTimer);
        nbt.putInt("t_int", interfaceStabilizedTimer);
        nbt.putInt("t_plate", plateStabilizedTimer);
        nbt.putInt("t_remote", remoteNoDrainTimer);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("experience");
        this.disconnected = nbt.getBoolean("disconnected");
        this.regenBlocked = nbt.getBoolean("regenBlocked");
        this.remoteAccessBlocked = nbt.getBoolean("remoteBlocked");
        this.pushbackEnabled = !nbt.contains("pushbackEnabled") || nbt.getBoolean("pushbackEnabled");
        this.stabilizedTimer = nbt.getInt("t_stab");
        this.interfaceStabilizedTimer = nbt.getInt("t_int");
        this.plateStabilizedTimer = nbt.getInt("t_plate");
        this.remoteNoDrainTimer = nbt.getInt("t_remote");
    }

    private boolean hasUnrestrictedItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof EnergyStabilizerItem && s.hasTag() && s.getOrCreateTag().getInt("Mode") == 3) return true;
        }
        return false;
    }
}