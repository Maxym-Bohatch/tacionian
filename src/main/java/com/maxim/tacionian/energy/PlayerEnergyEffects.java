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
        int percent = energy.getEnergyPercent();

        if (energy.isOverloaded()) {

            // 1. ШАНС ВИБУХУ (180%+)
            if (!isProtected && percent > 180 && energy.isDeadlyOverloadEnabled()) {
                if (level.random.nextFloat() < 0.005f) {
                    level.explode(null, player.getX(), player.getY(), player.getZ(), 3.0f, false, Level.ExplosionInteraction.NONE);
                    player.hurt(getTachyonDamage(player), Float.MAX_VALUE);
                    energy.setEnergy(0);
                    energy.sync(player);
                    return;
                }
            }

            // 2. ЗВУКИ (Прискорюються від відсотка)
            // Чим ближче до 200%, тим менший інтервал між звуками (мінімум 4 тіки)
            int soundInterval = Math.max(4, 25 - (percent - 100) / 4);
            if (player.tickCount % soundInterval == 0) {
                float volume = percent > 150 ? 1.0f : 0.5f;
                float pitch = 0.5f + (percent / 150f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, volume, pitch);
            }

            // 3. ЧАСТКИ (Швидкість розльоту та кількість залежать від відсотка)
            if (player.tickCount % (percent > 160 ? 2 : 5) == 0) {
                float speed = (percent - 100) / 50f; // Чим більше енергії, тим швидше розлітаються іскри
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        percent > 150 ? 12 : 5, // Більше часток при сильному перевантаженні
                        0.3, 0.3, 0.3,
                        speed);
            }

            // 4. НЕГАТИВНІ ЕФЕКТИ (Твої оригінальні налаштування)
            if (!isProtected) {
                if (percent > 110) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
                }

                if (percent > 140) {
                    if (player.tickCount % 40 == 0) {
                        player.setSecondsOnFire(1);
                        float dmg = 1.0f + (energy.getLevel() * 0.05f);
                        player.hurt(getTachyonDamage(player), dmg);
                    }
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
                }

                if (percent > 170) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, false));
                }
            }

            // Швидший дим при перевантаженні
            if (player.tickCount % 8 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 1, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // КРИТИЧНО НИЗЬКИЙ РІВЕНЬ
        if (energy.isCriticalLow() && player.tickCount % 100 == 0) {
            player.hurt(getTachyonDamage(player), 1.0f);
        }
    }
}