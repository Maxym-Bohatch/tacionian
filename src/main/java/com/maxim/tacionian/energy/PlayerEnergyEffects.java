package com.maxim.tacionian.energy;

import com.maxim.tacionian.api.effects.ITachyonEffect;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class PlayerEnergyEffects {
    public static final ResourceKey<DamageType> TACHYON_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "energy"));

    public static DamageSource getTachyonDamage(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TACHYON_DAMAGE_TYPE));
    }

    public static void apply(ServerPlayer player, PlayerEnergy energy) {
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        float ratio = energy.getEnergyFraction();

        // --- ЕФЕКТИ ПОНАД 100% (БЛАКИТНЕ ПОЛЕ) ---
        if (ratio > 1.05f) {
            // 1. Візуал поля
            if (player.tickCount % 5 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.5, 0.5, 0.5, 0.02);
            }

            // 2. Антигравітація (піднімає над землею)
            if (!hasSuitsProtection(player)) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, 0.02, 0));
                player.fallDistance = 0;
            }

            // 3. Відштовхування мобів
            float pushRadius = 3.0f + (ratio - 1.0f) * 5.0f;
            AABB area = player.getBoundingBox().inflate(pushRadius);
            for (Entity entity : level.getEntities(player, area)) {
                double dx = entity.getX() - player.getX();
                double dz = entity.getZ() - player.getZ();
                entity.push(dx * 0.2, 0.1, dz * 0.2);
            }
        }

        // --- ЛОГІКА ПЕРЕВАНТАЖЕННЯ (Стандартна) ---
        if (energy.isOverloaded()) {
            boolean isProtected = energy.isStabilized() || energy.isInterfaceStabilized() || energy.isPlateStabilized() || hasAddonProtection(player);

            // Іскри ростуть з енергією
            int pCount = (int)((ratio - 0.7f) * 20);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2, player.getZ(), Math.max(1, pCount), 0.3, 0.5, 0.3, 0.05);

            if (!isProtected) {
                if (ratio > 0.95f && player.tickCount % 20 == 0) {
                    player.hurt(getTachyonDamage(player), 2.0f);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                }
            }
        }
    }

    public static boolean hasAddonProtection(ServerPlayer player) {
        return player.getActiveEffects().stream().anyMatch(e -> e.getEffect() instanceof ITachyonEffect);
    }

    private static boolean hasSuitsProtection(ServerPlayer player) {
        // Тут пізніше додаси перевірку на броню/костюми з аддонів
        return false;
    }
}