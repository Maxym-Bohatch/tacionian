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

import com.maxim.tacionian.register.ModDamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "tacionian")
public class TachyonEnergyHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.player;
        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {

            float ratio = (float) pEnergy.getEnergy() / pEnergy.getMaxEnergy();

            // 1. ПЕРЕВІРКА СТАБІЛІЗАЦІЇ (Критичний момент)
            // Якщо енергія вище 100%, а стабілізатора/плити немає
            if (ratio > 1.0f && !pEnergy.isPlateStabilized() && !pEnergy.isRemoteNoDrain()) {
                // Шанс вибуху росте з кожним відсотком перевантаження
                if (player.getRandom().nextFloat() < (ratio - 1.0f) * 0.15f) {
                    triggerDynamicExplosion(player, ratio);
                    return;
                }
                // Наносимо періодичну шкоду, якщо ще не бабахнуло
                if (player.tickCount % 10 == 0) {
                    player.hurt(ModDamageSources.getTachyonDamage(player.level()), ratio * 1.5f);
                }
            }

            // 2. ДИНАМІЧНІ ЕФЕКТИ (Турбо-режим)
            if (ratio > 1.1f) {
                applyAura(player, ratio);
            }

            if (ratio > 1.35f) {
                applyMagnet(player);
                applyTurboPhysics(player); // Стрибки/Левітація
            }

            if (ratio > 1.8f) {
                applyCriticalChaos(player); // Руйнування скла/іскри
            }

            // ОЧИЩЕННЯ ПРАПОРЦІВ (Важливо!)
            // Плита і предмет ставлять їх у true кожен тік. Якщо гравець зійшов/сховав предмет,
            // наступного тіку тут буде false, і спрацює перевірка на вибух.
            pEnergy.setPlateStabilized(false);
            pEnergy.setRemoteNoDrain(false);
        });
    }

    private static void triggerDynamicExplosion(ServerPlayer player, float ratio) {
        // Сила вибуху: на 110% це ~4.4, на 200% це 8.0 (як ліжко в Енді)
        float power = 3.0f + (ratio * 2.5f);
        player.level().explode(player, player.getX(), player.getY(), player.getZ(), power, Level.ExplosionInteraction.BLOCK);
        player.hurt(ModDamageSources.getTachyonDamage(player.level()), Float.MAX_VALUE);
    }

    private static void applyAura(ServerPlayer player, float ratio) {
        double range = 1.5 + (ratio * 2.0);
        player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range)).forEach(e -> {
            if (e != player) {
                Vec3 dir = e.position().subtract(player.position()).normalize();
                e.knockback(0.3, -dir.x, -dir.z);
            }
        });
    }

    private static void applyMagnet(ServerPlayer player) {
        player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(5.0)).forEach(item -> {
            Vec3 pull = player.position().subtract(item.position()).normalize().scale(0.12);
            item.setDeltaMovement(item.getDeltaMovement().add(pull));
        });
    }

    private static void applyTurboPhysics(ServerPlayer player) {
        Vec3 v = player.getDeltaMovement();
        if (v.y < -0.1) {
            player.setDeltaMovement(v.x, v.y * 0.8, v.z);
            player.fallDistance = 0;
        }
        if (player.isSprinting()) {
            player.setDeltaMovement(v.add(player.getLookAngle().scale(0.05)));
        }
    }

    private static void applyCriticalChaos(ServerPlayer player) {
        if (player.tickCount % 5 == 0) {
            ((ServerLevel)player.level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1, player.getZ(), 6, 0.3, 0.5, 0.3, 0.05);
        }
    }
}