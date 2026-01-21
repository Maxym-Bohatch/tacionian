package com.maxim.tacionian.energy;

import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class PlayerEnergyEffects {
    // Ця константа потрібна для ModEvents, щоб розпізнавати тахіонну шкоду
    public static final ResourceKey<DamageType> TACHYON_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "energy"));

    // Метод для створення джерела шкоди (використовується нижче)
    public static DamageSource getTachyonDamage(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TACHYON_DAMAGE_TYPE));
    }

    public static void apply(ServerPlayer player, PlayerEnergy energy) {
        ServerLevel level = player.serverLevel();
        int percent = energy.getEnergyPercent();
        boolean isProtected = energy.isStabilized() || energy.isRemoteStabilized();

        // --- ЛОГІКА ПЕРЕВАНТАЖЕННЯ (80% - 100%) ---
        if (energy.isOverloaded()) {
            // 1. Звуковий супровід (частішає при наближенні до 100%)
            int soundInterval = Math.max(2, 25 - (percent - 80));
            if (player.tickCount % soundInterval == 0) {
                float pitch = 0.5f + (percent / 100f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.5f, pitch);
            }

            // 2. Негативні ефекти (якщо гравець не під стабілізатором)
            if (!isProtected) {
                // Фізична тряска (85%+)
                if (percent > 85 && player.tickCount % 5 == 0) {
                    player.push((level.random.nextFloat() - 0.5) * 0.15, 0, (level.random.nextFloat() - 0.5) * 0.15);
                    player.hurtMarked = true;
                }

                // Шкода, вогонь та дезорієнтація (90%+)
                if (percent > 90 && player.tickCount % 20 == 0) {
                    player.hurt(getTachyonDamage(player), 1.0f);
                    player.setSecondsOnFire(2);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                }
            }

            // 3. Візуальні частки (електричні розряди навколо гравця)
            int particleCount = (percent - 70) / 5;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2, player.getZ(),
                    particleCount, 0.3, 0.5, 0.3, 0.05);
        }

        // --- ЛОГІКА КРИТИЧНО НИЗЬКОГО ЗАРЯДУ ---
        if (energy.isCriticalLow() && player.tickCount % 80 == 0) {
            // При низькій енергії просто накладаємо втому
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0));
        }
    }
}