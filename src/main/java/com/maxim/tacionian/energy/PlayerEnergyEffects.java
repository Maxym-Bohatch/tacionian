/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.maxim.tacionian.energy;

import com.maxim.tacionian.items.energy.EnergyStabilizerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PlayerEnergyEffects {
    public static final ResourceKey<DamageType> TACHYON_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "energy"));

    public static void apply(ServerPlayer player, PlayerEnergy energy) {
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        float ratio = energy.getEnergyFraction();

        // 1. ЕФЕКТ ДЕФІЦИТУ
        if (ratio < 0.05f) {
            if (player.tickCount % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1));
                if (ratio < 0.01f) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            }
            return;
        }

        // 2. ВИЗНАЧЕННЯ СТАБІЛІЗАЦІЇ
        boolean isNovice = energy.getLevel() >= 1 && energy.getLevel() <= 5;
        boolean isSafeZone = energy.isStabilizedLogicActive() || hasUnrestrictedItem(player) || isNovice;

        float startThreshold = isSafeZone ? 1.89f : 0.89f;

        // 3. ЕФЕКТИ ПЕРЕВАНТАЖЕННЯ
        if (ratio > startThreshold) {
            float severity = Math.min(1.0f, (ratio - startThreshold) / 0.15f);

            // Візуал навколо гравця
            if (player.tickCount % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2, player.getZ(), (int)(severity * 10), 0.3, 0.5, 0.3, 0.05);
            }

            if (!isSafeZone && player.tickCount % 20 == 0) {
                if (severity > 0.7f) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            }

            // --- ФІЗИКА: Векторне відштовхування та Левітація ---
            // ПРИМІТКА: Додана перевірка physicsDisabled (твоя майбутня опція для аддонів)
            if (severity > 0.1f && !energy.isPhysicsDisabled()) {
                double forceX = 0, forceY = 0, forceZ = 0;
                float radius = 1.5f;

                double multiplier = isSafeZone ? 0.01 : 0.07;
                double energyPower = Math.min(2.0, (energy.getEnergy() / 1000.0) * severity * multiplier);

                for (int x = -2; x <= 2; x++) {
                    for (int y = -1; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                            BlockPos targetPos = player.blockPosition().offset(x, y, z);
                            if (!level.getBlockState(targetPos).isAir()) {
                                double dx = player.getX() - (targetPos.getX() + 0.5);
                                double dy = (player.getY() + 1.0) - (targetPos.getY() + 0.5);
                                double dz = player.getZ() - (targetPos.getZ() + 0.5);

                                double distSq = dx * dx + dy * dy + dz * dz;
                                if (distSq > 0 && distSq < 5.0) {
                                    double dist = Math.sqrt(distSq);
                                    double pushFactor = (radius + 1 - dist) / (radius + 1);

                                    forceX += (dx / dist) * pushFactor * energyPower;
                                    forceZ += (dz / dist) * pushFactor * energyPower;

                                    if (!isSafeZone) {
                                        forceY += (dy / dist) * pushFactor * energyPower;
                                    }

                                    // --- ВІЗУАЛ: Іскри на поверхні блоків ---
                                    if (player.tickCount % 4 == 0) {
                                        // Зміщуємо іскру на край блоку в сторону гравця, щоб її було видно
                                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                                targetPos.getX() + 0.5 + (dx/dist * -0.45),
                                                targetPos.getY() + 0.5 + (dy/dist * -0.45),
                                                targetPos.getZ() + 0.5 + (dz/dist * -0.45),
                                                1, 0.02, 0.02, 0.02, 0.1);
                                    }
                                }
                            }
                        }
                    }
                }

                // ЕФЕКТ ЛЕВІТАЦІЇ (Утримання висоти)
                if (isSafeZone) {
                    Vec3 currentVel = player.getDeltaMovement();
                    if (currentVel.y < 0) {
                        forceY = 0.082; // Компенсація гравітації
                    } else if (currentVel.y > 0.04) {
                        forceY = -0.015; // М'яке обмеження зльоту
                    }
                }

                // Жорсткий Clamp
                double limit = isSafeZone ? 0.12 : 0.35;
                forceX = Math.max(-limit, Math.min(limit, forceX));
                forceZ = Math.max(-limit, Math.min(limit, forceZ));
                forceY = Math.max(-0.08, Math.min(limit, forceY));

                player.setDeltaMovement(player.getDeltaMovement().add(forceX, forceY, forceZ));
                player.hurtMarked = true;
            }
        }
    }

    private static boolean hasUnrestrictedItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof EnergyStabilizerItem) {
                // Використовуємо getOrCreateTag для стабільності
                if (stack.hasTag() && stack.getOrCreateTag().getInt("Mode") == 3) return true;
            }
        }
        return false;
    }
}