/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GNU General Public License v3
 */

package com.maxim.tacionian.energy;

import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PlayerEnergyEffects {
    public static final ResourceKey<DamageType> TACHYON_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "energy"));

    public static void apply(ServerPlayer player, PlayerEnergy energy) {
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        float ratio = energy.getEnergyFraction();

        // 1. ЕФЕКТ ДЕФІЦИТУ (Локалізовані повідомлення)
        if (ratio < 0.05f) {
            if (player.tickCount % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, false));

                if (ratio < 0.01f) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
                    if (player.tickCount % 100 == 0) {
                        player.displayClientMessage(Component.translatable("message.tacionian.energy_depleted"), true);
                    }
                }
            }
            return;
        }

        // 2. ВИЗНАЧЕННЯ СТАТУСУ БЕЗПЕКИ
        boolean isSafeZone = energy.isStabilizedLogicActive() || hasUnrestrictedItem(player);
        float startThreshold = isSafeZone ? 1.85f : 0.88f;

        // 3. ЕФЕКТИ ПЕРЕВАНТАЖЕННЯ ТА ФІЗИКА
        if (ratio > startThreshold) {
            float severity = Math.min(1.0f, (ratio - startThreshold) / 0.15f);

            // Візуал навколо гравця
            if (player.tickCount % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2, player.getZ(), (int)(severity * 8), 0.3, 0.5, 0.3, 0.02);
            }

            // Накладання негативних ефектів (тільки якщо не в SafeZone)
            if (!isSafeZone && player.tickCount % 20 == 0) {
                if (severity > 0.5f) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
                if (severity > 0.8f) player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false));
            }

            // --- МАГНІТНА ФІЗИКА ---
            if (severity > 0.05f && !energy.isPhysicsDisabled()) {
                handleTachyonPhysics(player, level, energy, severity, isSafeZone);
            }
        }
    }

    private static void handleTachyonPhysics(ServerPlayer player, ServerLevel level, PlayerEnergy energy, float severity, boolean isSafeZone) {
        double forceX = 0, forceY = 0, forceZ = 0;
        double multiplier = isSafeZone ? 0.012 : 0.075;
        double energyPower = Math.min(2.5, (energy.getEnergy() / 1000.0) * severity * multiplier);

        // Оптимізований пошук блоків (обмежений радіус)
        BlockPos center = player.blockPosition();
        for (BlockPos targetPos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 2, 2))) {
            if (!level.getBlockState(targetPos).isAir()) {
                double dx = player.getX() - (targetPos.getX() + 0.5);
                double dy = (player.getY() + 1.0) - (targetPos.getY() + 0.5);
                double dz = player.getZ() - (targetPos.getZ() + 0.5);

                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > 0 && distSq < 6.0) {
                    double dist = Math.sqrt(distSq);
                    double pushFactor = (2.5 - dist) / 2.5;

                    forceX += (dx / dist) * pushFactor * energyPower;
                    forceZ += (dz / dist) * pushFactor * energyPower;
                    if (!isSafeZone) forceY += (dy / dist) * pushFactor * energyPower;

                    // Візуальні розряди на блоках
                    if (player.getRandom().nextFloat() < 0.1f) {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                targetPos.getX() + 0.5 + (dx/dist * -0.4),
                                targetPos.getY() + 0.5 + (dy/dist * -0.4),
                                targetPos.getZ() + 0.5 + (dz/dist * -0.4),
                                1, 0, 0, 0, 0);
                    }
                }
            }
        }

        // Логіка стабільної левітації
        if (isSafeZone) {
            Vec3 vel = player.getDeltaMovement();
            double hoverTarget = 0.08;
            if (vel.y < 0) forceY = 0.085; // Анти-гравітація
            else if (vel.y > hoverTarget) forceY = -0.02; // М'яке гальмування
        }

        // Clamp (Обмеження сили, щоб не викидало з карти)
        double limit = isSafeZone ? 0.15 : 0.4;
        forceX = Math.max(-limit, Math.min(limit, forceX));
        forceZ = Math.max(-limit, Math.min(limit, forceZ));
        forceY = Math.max(-0.1, Math.min(limit, forceY));

        player.push(forceX, forceY, forceZ);
        player.hurtMarked = true;
    }

    private static boolean hasUnrestrictedItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                if (stack.hasTag() && stack.getOrCreateTag().getInt("Mode") == 3) return true;
            }
        }
        return false;
    }
}