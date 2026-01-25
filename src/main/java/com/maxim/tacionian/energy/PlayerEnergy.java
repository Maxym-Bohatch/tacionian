/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.maxim.tacionian.energy;

import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.config.TacionianConfig;
import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import com.maxim.tacionian.network.EnergySyncPacket;
import com.maxim.tacionian.network.NetworkHandler;
import com.maxim.tacionian.register.ModDamageSources;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

public class PlayerEnergy {
    // --- ПРИВАТНІ ПОЛЯ ДАНИХ ---
    private int energy = 0;
    private int level = 1;
    private int experience = 0;
    private float fractionalExperience = 0.0f;

    private boolean disconnected = false;
    private boolean physicsDisabled = false;
    private boolean pushbackEnabled = true;
    private boolean regenBlocked = false;

    private int stabilizedTimer = 0;
    private int interfaceStabilizedTimer = 0;
    private int plateStabilizedTimer = 0;
    private int remoteNoDrainTimer = 0;

    // --- БАЗОВІ ГЕТТЕРИ ТА СЕТТЕРИ (ДЛЯ ВСІХ КЛАСІВ) ---
    public int getEnergy() { return energy; }
    public void setEnergy(int amount) { this.energy = Math.max(0, amount); }

    public int getLevel() { return level; }
    public void setLevel(int l) { this.level = Math.min(Math.max(1, l), TacionianConfig.MAX_LEVEL.get()); }

    public int getMaxEnergy() {
        return 1000 + (level - 1) * TacionianConfig.ENERGY_PER_LEVEL.get();
    }

    public int getExperience() { return experience; }
    public void setExperience(int exp) { this.experience = exp; }

    public int getRequiredExp() {
        return 5000 + (level * 1500);
    }

    // --- МЕТОДИ ДЛЯ HUD ТА КОНТРОЛЕРІВ (FIX COMPILATION) ---
    public float getEnergyFraction() {
        return getMaxEnergy() > 0 ? (float) energy / (float) getMaxEnergy() : 0.0f;
    }

    public int getEnergyPercent() {
        return (int) (getEnergyFraction() * 100);
    }

    public boolean isOverloaded() {
        return getEnergyFraction() > 1.0f;
    }

    public float getExpRatio() {
        return (float) experience / (float) getRequiredExp();
    }

    // --- КЕРУВАННЯ СТАТУСАМИ ТА ТАЙМЕРАМИ ---
    public boolean isDisconnected() { return disconnected; }
    public void setDisconnected(boolean state) { this.disconnected = state; }

    public boolean isPhysicsDisabled() { return physicsDisabled; }
    public void setPhysicsDisabled(boolean state) { this.physicsDisabled = state; }

    public boolean isPushbackEnabled() { return pushbackEnabled; }
    public void setPushbackEnabled(boolean enabled) { this.pushbackEnabled = enabled; }

    public boolean isRegenBlocked() { return regenBlocked; }
    public void setRegenBlocked(boolean state) { this.regenBlocked = state; }

    public void setStabilized(boolean v) { if (v) stabilizedTimer = 25; }
    public boolean isStabilized() { return stabilizedTimer > 0; }

    public void setInterfaceStabilized(boolean v) { if (v) interfaceStabilizedTimer = 25; }
    public boolean isInterfaceStabilized() { return interfaceStabilizedTimer > 0; }

    public void setPlateStabilized(boolean v) { if (v) plateStabilizedTimer = 25; }
    public boolean isPlateStabilized() { return plateStabilizedTimer > 0; }

    public void setRemoteNoDrain(boolean v) { if (v) remoteNoDrainTimer = 30; }
    public boolean isRemoteNoDrain() { return remoteNoDrainTimer > 0; }

    // ГОЛОВНА ЛОГІКА ЗАХИСТУ (Єдине джерело істини для всіх класів)
    public boolean isStabilizedLogicActive() {
        return (this.level <= 5) || isStabilized() || isInterfaceStabilized() || isPlateStabilized() || isRemoteNoDrain();
    }

    // --- СИСТЕМА ДОСВІДУ ---
    public void addExperience(float amount, ServerPlayer player) {
        this.fractionalExperience += amount;
        if (this.fractionalExperience >= 1.0f) {
            int toAdd = (int) this.fractionalExperience;
            this.experience += toAdd;
            this.fractionalExperience -= toAdd;

            while (this.experience >= getRequiredExp() && level < TacionianConfig.MAX_LEVEL.get()) {
                this.experience -= getRequiredExp();
                this.level++;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.MODE_SWITCH.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            sync(player);
        }
    }

    // --- РОБОТА З ЕНЕРГІЄЮ (БЕЗПЕЧНА) ---
    public int receiveEnergyPure(int amount, boolean simulate) {
        return receiveEnergyPure(amount, simulate, false);
    }

    public int receiveEnergyPure(int amount, boolean simulate, boolean isUnrestrictedPassed) {
        int absoluteMax = getMaxEnergy();
        boolean protectedMode = isUnrestrictedPassed || isStabilizedLogicActive();

        // Для новачків та захищених - поріг 1.99, для інших - практично ліміт
        float multiplier = protectedMode ? 1.99f : (level > 5 ? 1.05f : 0.9f);
        int hardCap = (int)(absoluteMax * multiplier);

        if (this.energy >= hardCap) return 0;
        int toAdd = Math.min(amount, hardCap - this.energy);

        if (!simulate && toAdd > 0) {
            this.energy += toAdd;
        }
        return toAdd;
    }

    public int extractEnergyPure(int amount, boolean simulate) {
        int toExt = Math.min(amount, this.energy);
        if (!simulate && toExt > 0) {
            this.energy -= toExt;
        }
        return toExt;
    }

    // --- ГОЛОВНИЙ ОБРОБНИК (TICK) ---
    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // Таймери
        if (stabilizedTimer > 0) stabilizedTimer--;
        if (interfaceStabilizedTimer > 0) interfaceStabilizedTimer--;
        if (plateStabilizedTimer > 0) plateStabilizedTimer--;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;

        float ratio = getEnergyFraction();
        boolean protectedStatus = isStabilizedLogicActive() || hasUnrestrictedItem(player);
        float dangerLimit = protectedStatus ? 1.99f : 0.99f;

        // 1. Ефекти перевантаження
        if (ratio > 1.0f && !player.isCreative()) {
            if (pushbackEnabled && !physicsDisabled) applyPushbackEffect(player, ratio);

            if (player.tickCount % 10 == 0) {
                ((ServerLevel)player.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
            }

            if (player.tickCount % 20 == 0) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.6f, 1.0f);

                int leak = 5 + (int)((ratio - 1.0f) * 20);
                this.extractEnergyPure(leak, false);
                MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(player.level(), player.blockPosition(), leak));
            }
        }

        // 2. Звук заряду перед вибухом
        if (ratio > (dangerLimit - 0.25f) && ratio <= dangerLimit && player.tickCount % 6 == 0) {
            float pitch = 0.5f + (ratio - (dangerLimit - 0.25f)) * 4.0f;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ENERGY_CHARGE.get(), SoundSource.PLAYERS, 0.8f, Math.min(pitch, 2.0f));
        }

        // 3. Колапс
        if (ratio > dangerLimit && !player.isCreative()) {
            triggerInstantCollapse(serverPlayer, ratio);
            return;
        }

        processPassiveLogic(serverPlayer, protectedStatus, ratio);
    }

    private void applyPushbackEffect(Player player, float ratio) {
        Vec3 look = player.getLookAngle();
        double strength = Math.min((ratio - 1.0) * 0.12, 0.15);
        double yPush = (player.getDeltaMovement().y < 0.1) ? 0.02 : 0.0;
        player.push(-look.x * strength, yPush, -look.z * strength);
        player.hurtMarked = true;
    }

    private void processPassiveLogic(ServerPlayer player, boolean isProtected, float ratio) {
        float erosionThreshold = isProtected ? 1.85f : 0.85f;

        if (ratio > erosionThreshold && !player.isCreative()) {
            this.experience -= (int) ((ratio - erosionThreshold) * 45.0f);
            if (this.experience < 0) {
                if (this.level > 1) {
                    this.level--;
                    this.experience = getRequiredExp() / 2;
                } else this.experience = 0;
            }
            if (player.tickCount % 10 == 0) sync(player);
        }

        if (player.tickCount % 20 == 0 && !regenBlocked && ratio < (isProtected ? 1.80f : 1.0f)) {
            receiveEnergyPure(15 + (level * 3), false, isProtected);
            sync(player);
        }
    }

    private void triggerInstantCollapse(ServerPlayer player, float ratio) {
        player.level().explode(null, player.getX(), player.getY(), player.getZ(),
                4.5f * ratio, true, Level.ExplosionInteraction.TNT);
        this.level = Math.max(1, this.level - 2);
        this.experience = 0;
        this.energy = 0;
        player.hurt(ModDamageSources.getTachyonDamage(player.level()), Float.MAX_VALUE);
        sync(player);
    }

    private boolean hasUnrestrictedItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof EnergyStabilizerItem) {
                if (stack.hasTag() && stack.getTag().getInt("Mode") == 3) return true;
            }
        }
        return false;
    }

    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("level", level);
        nbt.putInt("exp", experience);
        nbt.putFloat("fractionalExp", fractionalExperience);
        nbt.putBoolean("disconnected", disconnected);
        nbt.putBoolean("physicsDisabled", physicsDisabled);
        nbt.putBoolean("pushbackEnabled", pushbackEnabled);
        nbt.putBoolean("regenBlocked", regenBlocked);
        nbt.putInt("stabTimer", stabilizedTimer);
        nbt.putInt("intStabTimer", interfaceStabilizedTimer);
        nbt.putInt("plateStabTimer", plateStabilizedTimer);
        nbt.putInt("remoteNoDrain", remoteNoDrainTimer);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("exp");
        this.fractionalExperience = nbt.getFloat("fractionalExp");
        this.disconnected = nbt.getBoolean("disconnected");
        this.physicsDisabled = nbt.getBoolean("physicsDisabled");
        this.regenBlocked = nbt.getBoolean("regenBlocked");
        this.stabilizedTimer = nbt.getInt("stabTimer");
        this.interfaceStabilizedTimer = nbt.getInt("intStabTimer");
        this.plateStabilizedTimer = nbt.getInt("plateStabTimer");
        this.remoteNoDrainTimer = nbt.getInt("remoteNoDrain");
        if (nbt.contains("pushbackEnabled")) {
            this.pushbackEnabled = nbt.getBoolean("pushbackEnabled");
        }
    }
}