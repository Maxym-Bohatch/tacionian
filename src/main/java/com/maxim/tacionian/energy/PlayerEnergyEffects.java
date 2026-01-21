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

            // 1. ШАНС ВИБУХУ (Тільки вище 180% і якщо не захищений)
            // Замість миттєвої смерті на 150%, даємо шанс вижити
            if (!isProtected && percent > 180 && energy.isDeadlyOverloadEnabled()) {
                // Шанс вибуху ~0.5% кожного тіку. В середньому це 10 секунд життя.
                if (level.random.nextFloat() < 0.005f) {
                    level.explode(null, player.getX(), player.getY(), player.getZ(), 3.0f, false, Level.ExplosionInteraction.NONE);
                    player.hurt(getTachyonDamage(player), Float.MAX_VALUE);
                    energy.setEnergy(0);
                    energy.sync(player);
                    return;
                }
            }

            // 2. ЗВУКИ ТА ВІЗУАЛ (Наростають від відсотка)
            if (player.tickCount % (percent > 150 ? 5 : 20) == 0) {
                float volume = percent > 150 ? 0.9f : 0.4f;
                float pitch = 0.5f + (percent / 200f); // Чим більше енергії, тим вищий писк
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, volume, pitch);
            }

            // 3. НЕГАТИВНІ ЕФЕКТИ (Ступеневі)
            if (!isProtected) {
                // Стадія 1: Понад 110% - легке сповільнення
                if (percent > 110) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
                }

                // Стадія 2: Понад 140% - вогонь та посилена шкода
                if (percent > 140) {
                    if (player.tickCount % 40 == 0) { // Шкода раз на 2 секунди, а не на 1
                        player.setSecondsOnFire(1);
                        // Полегшена формула шкоди: 1.0 + рівень * 0.05
                        float dmg = 1.0f + (energy.getLevel() * 0.05f);
                        player.hurt(getTachyonDamage(player), dmg);
                    }
                    // Додаємо нудоту, щоб було важче йти
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
                }

                // Стадія 3: Понад 170% - "Тремтіння" (затемнення або слабкість)
                if (percent > 170) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, false));
                    if (player.tickCount % 5 == 0) {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, 0.5, 0.5, 0.2);
                    }
                }
            }

            // Постійні частинки при перевантаженні
            if (player.tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 2, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // КРИТИЧНО НИЗЬКИЙ РІВЕНЬ (Більш гуманно)
        if (energy.isCriticalLow() && player.tickCount % 100 == 0) {
            player.hurt(getTachyonDamage(player), 1.0f);
        }
    }
}