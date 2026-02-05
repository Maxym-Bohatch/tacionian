/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GNU General Public License v3
 */

package com.maxim.tacionian.events;

import com.maxim.tacionian.Tacionian;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.energy.PlayerEnergyEffects;
import com.maxim.tacionian.energy.control.EnergyControlResolver;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Tacionian.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerEnergyProvider.PLAYER_ENERGY).isPresent()) {
                event.addCapability(new ResourceLocation(Tacionian.MOD_ID, "properties"), new PlayerEnergyProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        // Обов'язково оживляємо капабіліті старого гравця для копіювання
        event.getOriginal().reviveCaps();

        event.getOriginal().getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(oldStore -> {
            event.getEntity().getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(newStore -> {
                CompoundTag nbt = new CompoundTag();
                oldStore.saveNBTData(nbt);
                newStore.loadNBTData(nbt);

                // Якщо це смерть (а не перехід між світами)
                if (event.isWasDeath()) {
                    // Обнуляємо тільки тимчасові статуси та енергію.
                    // Рівень залишається таким, яким він став після doCollapse()
                    newStore.setEnergy(0);
                    newStore.setInterfaceStabilized(false);
                    newStore.setPlateStabilized(false);
                    newStore.setStabilized(false);
                }

                // Синхронізуємо дані з клієнтом відразу після клонування
                if (event.getEntity() instanceof ServerPlayer sp) {
                    newStore.sync(sp);
                }
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> energy.sync(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DamageSource source = event.getSource();
            // Ефекти при смерті від тахіонів
            if (source.is(PlayerEnergyEffects.TACHYON_DAMAGE_TYPE)) {
                ServerLevel level = player.serverLevel();
                double x = player.getX();
                double y = player.getY() + 1.0;
                double z = player.getZ();

                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.PORTAL, x, y, z, 40, 0.2, 0.5, 0.2, 0.5);

                level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.5f);
                level.playSound(null, x, y, z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5f, 0.1f);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Захист на 3 секунди після спавну
            if (player.tickCount < 60) return;

            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                EnergyControlResolver.resolve(player, energy);
                energy.tick(player);
                PlayerEnergyEffects.apply(player, energy);

                // Регулярна синхронізація кожні 5 тіків
                if (player.level().getGameTime() % 5 == 0) {
                    energy.sync(player);
                }
            });
        }
    }
}