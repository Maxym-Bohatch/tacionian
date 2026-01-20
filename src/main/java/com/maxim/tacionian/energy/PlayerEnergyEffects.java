package com.maxim.tacionian.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceKey;

import java.util.Random;

public class PlayerEnergyEffects {

    private static final Random RANDOM = new Random();

    // Створюємо ключ для нашого типу шкоди (має збігатися з локалізацією tacionian.energy)
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

        // ==========================
        // ПЕРЕВАНТАЖЕННЯ (Нестабільність)
        // ==========================
        if (energy.isOverloaded()) {
            if (!isProtected) {
                if (player.tickCount % 20 == 0) {
                    player.setSecondsOnFire(2);

                    // ВИКОРИСТОВУЄМО ТАХІОННУ ШКОДУ ЗАМІСТЬ MAGIC
                    player.hurt(getTachyonDamage(player), 1.0f + (energy.getLevel() * 0.2f));

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.2f, 2.0f);
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
            }

            if (player.tickCount % 5 == 0) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        player.getX(), player.getY() + 1, player.getZ(),
                        8, 0.5, 0.7, 0.5, 0.1);

                if (RANDOM.nextFloat() > 0.7f) {
                    level.sendParticles(ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            10, 0.2, 0.2, 0.2, 0.5);
                }
            }
            if (isProtected) return;
        }

        // ==========================
        // КРИТИЧНО НИЗЬКО (Виснаження)
        // ==========================
        if (energy.isCriticalLow()) {
            if (player.tickCount % 40 == 0) {
                // ТУТ ТАКОЖ МОЖНА ВИКОРИСТОВУВАТИ ТАХІОННУ ШКОДУ
                // Або залишити starve(), якщо хочеш, щоб повідомлення було про голод
                player.hurt(getTachyonDamage(player), 1.0f);

                level.sendParticles(ParticleTypes.SQUID_INK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.3, 0.5, 0.3, 0.05);
            }
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
            return;
        }

        // ==========================
        // БОНУСИ ВИСОКОГО РІВНЯ
        // ==========================
        if (energy.getLevel() >= 5 && !energy.isOverloaded() && !energy.isCriticalLow()) {
            if (player.tickCount % 100 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, true, true));
                if (energy.getLevel() >= 10) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, true, true));
                }
            }

            if (player.tickCount % 20 == 0 && RANDOM.nextFloat() > 0.8f) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }
}