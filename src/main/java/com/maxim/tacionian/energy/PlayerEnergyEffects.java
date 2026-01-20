package com.maxim.tacionian.energy;

import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceKey;

import java.util.Random;

public class PlayerEnergyEffects {
    private static final Random RANDOM = new Random();

    public static final ResourceKey<DamageType> TACHYON_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "energy"));

    private static DamageSource getTachyonDamage(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TACHYON_DAMAGE_TYPE));
    }

    public static void apply(ServerPlayer player, PlayerEnergy energy) {
        ServerLevel level = player.serverLevel();
        boolean isProtected = energy.isStabilized() || energy.isRemoteStabilized();

        if (energy.isOverloaded()) {
            // ВЛАСНИЙ ЗВУК: Гудіння при оверлоаді (високий тон як сирена)
            if (player.tickCount % 20 == 0 && energy.getEnergyPercent() > 95) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.4f, 2.0f);
            }

            if (!isProtected) {
                if (player.tickCount % 20 == 0) {
                    player.setSecondsOnFire(2);
                    player.hurt(getTachyonDamage(player), 1.0f + (energy.getLevel() * 0.2f));
                    // Системний грім для ефекту удару
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.2f, 2.0f);
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
            }

            if (player.tickCount % 5 == 0) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1, player.getZ(), 8, 0.5, 0.7, 0.5, 0.1);
            }
        }

        if (energy.isCriticalLow() && player.tickCount % 40 == 0) {
            player.hurt(getTachyonDamage(player), 1.0f);
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1, player.getZ(), 5, 0.3, 0.5, 0.3, 0.05);
        }
    }
}