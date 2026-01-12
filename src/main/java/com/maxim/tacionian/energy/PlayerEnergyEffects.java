package com.maxim.tacionian.energy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Random;

public class PlayerEnergyEffects {

    private static final Random RANDOM = new Random();

    public static void apply(ServerPlayer player, PlayerEnergy energy) {
        ServerLevel level = player.serverLevel();
        boolean isProtected = energy.isStabilized() || energy.isRemoteStabilized();

        // ==========================
        // ПЕРЕВАНТАЖЕННЯ (Нестабільність)
        // ==========================
        if (energy.isOverloaded()) {
            if (!isProtected) {
                // Шкода та вогонь (Тільки якщо немає захисту)
                if (player.tickCount % 20 == 0) {
                    player.setSecondsOnFire(2);
                    player.hurt(player.damageSources().magic(), 1.0f + (energy.getLevel() * 0.2f));

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.2f, 2.0f);
                }
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
            }

            // ВІЗУАЛЬНИЙ ЕФЕКТ (Працює завжди при перевантаженні, як попередження)
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
            if (isProtected) return; // Якщо стабілізовано, інші негативні ефекти не перевіряємо
        }

        // ==========================
        // КРИТИЧНО НИЗЬКО (Виснаження)
        // ==========================
        if (energy.isCriticalLow()) {
            if (player.tickCount % 40 == 0) {
                player.hurt(player.damageSources().starve(), 1.0f);
                level.sendParticles(ParticleTypes.SQUID_INK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.3, 0.5, 0.3, 0.05);
            }
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
            return;
        }

        // ==========================
        // БОНУСИ ВИСОКОГО РІВНЯ (Level 5+)
        // ==========================
        // Надаються тільки якщо енергія стабільна (від 20% до 90%)
        if (energy.getLevel() >= 5 && !energy.isOverloaded() && !energy.isCriticalLow()) {
            // Пасивна регенерація здоров'я та стійкість
            if (player.tickCount % 100 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, true, true));

                // На 10 рівні даємо силу
                if (energy.getLevel() >= 10) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, true, true));
                }
            }

            // Легкий візуальний ефект успішної роботи ядра (блакитні іскри)
            if (player.tickCount % 20 == 0 && RANDOM.nextFloat() > 0.8f) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }
}