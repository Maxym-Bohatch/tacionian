package com.maxim.tacionian.energy;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.PacketDistributor;

public class PlayerEnergy {
    private int energy = 0;
    private int level = 1;
    private int experience = 0;
    private float fractionalExperience = 0.0f;
    private int customColor = -1;

    private int stabilizedTimer = 0;
    private int interfaceStabilizedTimer = 0;
    private int plateStabilizedTimer = 0;
    private int remoteNoDrainTimer = 0;

    private boolean disconnected = false;
    private boolean regenBlocked = false;

    public void setEnergy(int amount) { this.energy = Math.max(0, amount); }
    public void setLevel(int level) { this.level = Math.min(Math.max(1, level), TacionianConfig.MAX_LEVEL.get()); }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    public void setFractionalExperience(float f) { this.fractionalExperience = f; }
    public void setCustomColor(int color) { this.customColor = color; }
    public void setDisconnected(boolean state) { this.disconnected = state; }
    public boolean isDisconnected() { return disconnected; }
    public void setRegenBlocked(boolean state) { this.regenBlocked = state; }
    public boolean isRegenBlocked() { return regenBlocked; }

    public int getEnergy() { return energy; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getCustomColor() { return customColor; }

    public int getMaxEnergy() {
        return 1000 + (level - 1) * TacionianConfig.ENERGY_PER_LEVEL.get();
    }

    public float getEnergyFraction() { return getMaxEnergy() > 0 ? (float) energy / getMaxEnergy() : 0; }
    public int getEnergyPercent() { return (int)(getEnergyFraction() * 100); }

    public int getRequiredExp() { return level < 20 ? 500 + (level * 100) : 2500 + (level * 250); }
    public int getRegenRate() { return (int) (TacionianConfig.BASE_REGEN.get() + (level * 2.0)); }
    public boolean isOverloaded() {
        return getEnergyFraction() >= 0.90f;
    }
    public int receiveEnergyPure(int amount, boolean simulate) {
        int absoluteMax = getMaxEnergy();
        int hardCap = (this.level <= TacionianConfig.NOVICE_LEVEL_THRESHOLD.get()) ? (int)(absoluteMax * 0.85f) : absoluteMax * 2;
        if (this.energy >= hardCap) return 0;
        int toAdd = Math.min(amount, hardCap - this.energy);
        if (!simulate && toAdd > 0) this.energy += toAdd;
        return toAdd;
    }

    public int extractEnergyPure(int amount, boolean simulate) {
        int toExt = Math.min(amount, this.energy);
        if (!simulate && toExt > 0) this.energy -= toExt;
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
            }
            if (player != null) this.sync(player);
        }
    }

    public void sync(ServerPlayer player) {
        if (player != null && !player.level().isClientSide) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EnergySyncPacket(this));
        }
    }

    public void tick(Player player) {
        if (player.level().isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        float ratio = getEnergyFraction();

        if (stabilizedTimer > 0) stabilizedTimer--;
        if (interfaceStabilizedTimer > 0) interfaceStabilizedTimer--;
        if (plateStabilizedTimer > 0) plateStabilizedTimer--;
        if (remoteNoDrainTimer > 0) remoteNoDrainTimer--;

        if (!isPlateStabilized() && !isInterfaceStabilized()) {
            this.regenBlocked = false;
        }

        if (ratio > 1.05f) {
            if (isStabilizedLogicActive()) {
                applyTurboEffects(serverPlayer, ratio);
            } else if (player.tickCount % 20 == 0) {
                // Замість аларму — високий писк гудіння
                player.level().playSound(null, player.blockPosition(), ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.5f, 1.9f);
            }
        }

        if (ratio >= 1.0f && !player.isCreative() && !player.isSpectator()) {
            if (!isStabilizedLogicActive()) {
                if (ratio > 1.2f && player.getRandom().nextFloat() < (ratio - 1.0f) * 0.15f) {
                    triggerInstantCollapse(serverPlayer, ratio);
                    return;
                }
                if (ratio > 1.40f) {
                    player.level().explode(null, player.getX(), player.getY(), player.getZ(), 4.0f, true, Level.ExplosionInteraction.TNT);
                    player.hurt(ModDamageSources.getTachyonDamage(player.level()), Float.MAX_VALUE);
                    this.energy = 0;
                    this.sync(serverPlayer);
                } else if (ratio > 1.15f) {
                    // Замість звуку вибуху — низький звук зарядки
                    player.level().playSound(null, player.blockPosition(), ModSounds.ENERGY_CHARGE.get(), SoundSource.PLAYERS, 1.0f, 0.5f);
                    player.level().explode(null, player.getX(), player.getY(), player.getZ(), 2.5f, false, Level.ExplosionInteraction.BLOCK);
                    player.hurt(ModDamageSources.getTachyonDamage(player.level()), 12.0f);
                    this.energy = getMaxEnergy();
                    this.sync(serverPlayer);
                }
            }
        }

        if (player.tickCount % 20 == 0 && !regenBlocked) {
            int maxE = getMaxEnergy();
            int target = (this.level <= TacionianConfig.NOVICE_LEVEL_THRESHOLD.get()) ? (int)(maxE * 0.85f) : maxE;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.getItem() instanceof EnergyStabilizerItem) {
                    int mode = s.getOrCreateTag().getInt("Mode");
                    if (mode == 3 && this.level > TacionianConfig.NOVICE_LEVEL_THRESHOLD.get()) target = maxE * 2;
                    else target = Math.min(target, (maxE * EnergyStabilizerItem.getThresholdForMode(mode)) / 100);
                    break;
                }
            }
            if (this.energy < target) {
                if (receiveEnergyPure(getRegenRate(), false) > 0) this.sync(serverPlayer);
            }
        }
    }

    private boolean isStabilizedLogicActive() { return isStabilized() || isInterfaceStabilized() || isPlateStabilized() || isRemoteNoDrain(); }

    private void applyTurboEffects(ServerPlayer player, float ratio) {
        if (player.tickCount % 20 == 0) {
            // Низьке гудіння для турбо-аури
            player.level().playSound(null, player.blockPosition(), ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.35f, 0.6f);
        }
        double range = 1.0 + (ratio * 2.5);
        player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range)).forEach(e -> {
            if (e != player) {
                Vec3 dir = e.position().subtract(player.position()).normalize();
                e.knockback(0.3 * ratio, -dir.x, -dir.z);
            }
        });
        if (ratio > 1.3f) {
            player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(5.0 + ratio)).forEach(item -> {
                item.setDeltaMovement(item.getDeltaMovement().add(player.position().subtract(item.position()).normalize().scale(0.12)));
            });
            Vec3 v = player.getDeltaMovement();
            if (v.y < -0.1) { player.setDeltaMovement(v.x, v.y * 0.8, v.z); player.fallDistance = 0; }
        }
        if (ratio > 1.8f && player.tickCount % 5 == 0) {
            ((ServerLevel)player.level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.3, 0.4, 0.3, 0.05);
        }
    }

    private void triggerInstantCollapse(ServerPlayer player, float ratio) {
        player.level().playSound(null, player.blockPosition(), ModSounds.ENERGY_CHARGE.get(), SoundSource.PLAYERS, 1.2f, 0.4f);
        player.level().explode(null, player.getX(), player.getY(), player.getZ(), 3.0f * ratio, true, Level.ExplosionInteraction.TNT);
        player.hurt(ModDamageSources.getTachyonDamage(player.level()), Float.MAX_VALUE);
        this.energy = 0;
        this.sync(player);
    }

    public void setStabilized(boolean v) { if (v) stabilizedTimer = 25; }
    public boolean isStabilized() { return stabilizedTimer > 0; }
    public void setInterfaceStabilized(boolean v) { if (v) interfaceStabilizedTimer = 25; }
    public boolean isInterfaceStabilized() { return interfaceStabilizedTimer > 0; }
    public void setPlateStabilized(boolean v) { if (v) plateStabilizedTimer = 25; }
    public boolean isPlateStabilized() { return plateStabilizedTimer > 0; }
    public void setRemoteNoDrain(boolean v) { if (v) remoteNoDrainTimer = 30; }
    public boolean isRemoteNoDrain() { return remoteNoDrainTimer > 0; }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy); nbt.putInt("level", level); nbt.putInt("exp", experience);
        nbt.putFloat("fractionalExp", fractionalExperience); nbt.putInt("customColor", customColor);
        nbt.putBoolean("disconnected", disconnected); nbt.putBoolean("regenBlocked", regenBlocked);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.energy = nbt.getInt("energy"); this.level = Math.max(1, nbt.getInt("level"));
        this.experience = nbt.getInt("exp"); this.fractionalExperience = nbt.getFloat("fractionalExp");
        this.customColor = nbt.contains("customColor") ? nbt.getInt("customColor") : -1;
        this.disconnected = nbt.getBoolean("disconnected"); this.regenBlocked = nbt.getBoolean("regenBlocked");
    }

    public static class TachyonEnergyUpdateEvent extends Event {
        public final Player player; public final PlayerEnergy energy;
        public TachyonEnergyUpdateEvent(Player p, PlayerEnergy e) { this.player = p; this.energy = e; }
    }
}