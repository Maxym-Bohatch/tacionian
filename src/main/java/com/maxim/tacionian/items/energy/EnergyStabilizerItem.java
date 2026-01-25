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

package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyStabilizerItem extends Item {
    public EnergyStabilizerItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // Статус дистанційної стабілізації (захист від вибуху на низьких %)
                pEnergy.setRemoteNoDrain(true);

                // В інвентарі фізика ПРАЦЮЄ (pushback активний), щоб гравець міг перевантажуватися
                pEnergy.setPhysicsDisabled(false);

                int mode = stack.getOrCreateTag().getInt("Mode");
                int currentE = pEnergy.getEnergy();
                int maxE = pEnergy.getMaxEnergy();

                // Блокуємо регенерацію тільки в обмежених режимах
                int threshold = (maxE * getThresholdForMode(mode)) / 100;
                boolean shouldBlock = (mode != 3) && (currentE >= threshold);
                pEnergy.setRegenBlocked(shouldBlock);

                if (level.getGameTime() % 20 == 0) pEnergy.sync(player);
            });
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            int mode = (stack.getOrCreateTag().getInt("Mode") + 1) % 4;
            stack.getOrCreateTag().putInt("Mode", mode);
            level.playSound(null, player.blockPosition(), ModSounds.MODE_SWITCH.get(), SoundSource.PLAYERS, 0.6f, 0.8f + (mode * 0.1f));

            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName(mode)), true);
            }
            return InteractionResultHolder.success(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // ПРИ ПРОВЕДЕННІ СТАБІЛІЗАЦІЇ (ПКМ) — Вимикаємо відштовхування!
                pEnergy.setPhysicsDisabled(true);

                int mode = stack.getOrCreateTag().getInt("Mode");
                int maxE = pEnergy.getMaxEnergy();
                int thresholdValue = (mode == 3) ? 0 : (maxE * getThresholdForMode(mode)) / 100;

                if (pEnergy.getEnergy() > thresholdValue) {
                    // 1. Очищення дебафів
                    if (player.tickCount % 10 == 0) {
                        player.getActiveEffects().stream()
                                .filter(e -> !e.getEffect().isBeneficial())
                                .findFirst().ifPresent(effect -> {
                                    player.removeEffect(effect.getEffect());
                                    pEnergy.extractEnergyPure(200, false);
                                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.4f, 1.5f);
                                });
                    }

                    // 2. Злив енергії
                    int excess = pEnergy.getEnergy() - thresholdValue;
                    int toDrain = (mode == 3) ? 120 + (excess / 5) : 60 + (excess / 8);

                    pEnergy.extractEnergyPure(toDrain, false);
                    // Викидаємо енергію в атмосферу для аддонів
                    MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, player.blockPosition(), toDrain));

                    // 3. Ефекти та звуки
                    if (count % 5 == 0) {
                        float pitch = (mode == 3) ? 0.8f : 1.3f;
                        level.playSound(null, player.blockPosition(), ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.25f, pitch);

                        var particle = (mode == 3) ? ParticleTypes.SOUL : ParticleTypes.ELECTRIC_SPARK;
                        ((ServerLevel)level).sendParticles(particle,
                                player.getX(), player.getY() + 1.2, player.getZ(),
                                3, 0.2, 0.2, 0.2, 0.02);
                    }
                    pEnergy.sync(player);
                }
            });
        }
    }

    // Скидаємо статус вимкненої фізики, коли гравець перестав тиснути ПКМ
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                pEnergy.setPhysicsDisabled(false);
                pEnergy.sync(player);
            });
        }
    }

    public static int getThresholdForMode(int mode) {
        return switch (mode) {
            case 0 -> 75;
            case 1 -> 40;
            case 2 -> 15;
            default -> 100;
        };
    }

    private Component getModeName(int mode) {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe").withStyle(ChatFormatting.GREEN);
            case 1 -> Component.translatable("mode.tacionian.balanced").withStyle(ChatFormatting.YELLOW);
            case 2 -> Component.translatable("mode.tacionian.performance").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("mode.tacionian.unrestricted").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int mode = stack.hasTag() ? stack.getTag().getInt("Mode") : 0;
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.usage").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.mode").append(": ").append(getModeName(mode)));
    }

    @Override public int getUseDuration(ItemStack s) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
}