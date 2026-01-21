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
import net.minecraft.world.level.Level;

public class PlayerEnergyEffects {
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
            // ФІНАЛЬНИЙ ВИБУХ: Враховуємо новий прапорець безпеки
            if (!isProtected && energy.getEnergyPercent() > 150 && energy.isDeadlyOverloadEnabled()) {
                level.explode(null, player.getX(), player.getY(), player.getZ(), 4.0f, false, Level.ExplosionInteraction.NONE);
                player.hurt(getTachyonDamage(player), Float.MAX_VALUE);
                energy.setEnergy(0);
                energy.sync(player);
                return;
            }

            // Звуковий супровід
            boolean isCritical = energy.getEnergyPercent() > 95;
            if (player.tickCount % (isCritical ? 10 : 30) == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, isCritical ? 0.8f : 0.4f, isCritical ? 1.2f : 0.8f);
            }

            if (!isProtected) {
                if (player.tickCount % 20 == 0) {
                    player.setSecondsOnFire(2);
                    player.hurt(getTachyonDamage(player), 1.0f + (energy.getLevel() * 0.1f));
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
            }

            // Частинки
            if (player.tickCount % 5 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 5, 0.4, 0.4, 0.4, 0.1);
            }
        }

        if (energy.isCriticalLow() && player.tickCount % 60 == 0) {
            player.hurt(getTachyonDamage(player), 1.0f);
        }
    }
}