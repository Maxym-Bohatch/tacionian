package com.maxim.tacionian.energy;

import com.maxim.tacionian.api.effects.ITachyonEffect; // Імпортуємо інтерфейс
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
    public static final ResourceKey<DamageType> TACHYON_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "energy"));

    public static DamageSource getTachyonDamage(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TACHYON_DAMAGE_TYPE));
    }

    public static void apply(ServerPlayer player, PlayerEnergy energy) {
        // Креатив і глядач повністю ігнорують систему ефектів
        if (player.isCreative() || player.isSpectator()) return;
        if (energy.getEnergy() <= 0) {
            if (player.tickCount % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
            return;
        }
        ServerLevel level = player.serverLevel();
        int percent = energy.getEnergyPercent();

        // ПЕРЕВІРКА ЗАХИСТУ: Базові пристрої + Аддони
        boolean isProtected = energy.isStabilized() ||
                energy.isInterfaceStabilized() ||
                energy.isPlateStabilized() ||
                hasAddonProtection(player);

        // --- ЛОГІКА ПЕРЕВАНТАЖЕННЯ ---
        if (energy.isOverloaded()) {
            // Звук гудіння (Pitch росте разом з енергією)
            int soundInterval = Math.max(2, 25 - (percent - 80));
            if (player.tickCount % soundInterval == 0) {
                float pitch = 0.5f + (percent / 100f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.4f, pitch);
            }

            if (!isProtected) {
                // Тряска камери та відкидання
                if (percent > 85 && player.tickCount % 5 == 0) {
                    player.push((level.random.nextFloat() - 0.5) * 0.12, 0, (level.random.nextFloat() - 0.5) * 0.12);
                    player.hurtMarked = true;
                }

                // Смертельні ефекти (вогонь і шкода)
                if (percent > 90 && player.tickCount % 20 == 0) {
                    player.hurt(getTachyonDamage(player), 2.0f); // Збільшено шкоду
                    player.setSecondsOnFire(3);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                }
            }

            // Електричні розряди (видимі навіть із захистом, як індикатор потужності)
            int particleCount = (percent - 70) / 4;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2, player.getZ(),
                    particleCount, 0.4, 0.6, 0.4, 0.05);
        }

        // --- КРИТИЧНО НИЗЬКИЙ ЗАРЯД ---
        if (energy.isCriticalLow() && player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
    }

    /**
     * Перевіряє, чи має гравець активні ефекти з аддонів, які дають стабілізацію.
     */
    private static boolean hasAddonProtection(ServerPlayer player) {
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect() instanceof ITachyonEffect) {
                return true; // Якщо це тахіонний ефект, вважаємо його захисним
            }
        }
        return false;
    }
}