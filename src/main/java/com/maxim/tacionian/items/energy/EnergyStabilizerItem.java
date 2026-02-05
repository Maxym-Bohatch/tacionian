/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyStabilizerItem extends Item {
    public static final int MAX_INTERNAL_ENERGY = 10000;

    public EnergyStabilizerItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.RARE).durability(1000));
    }

    private boolean isBestStabilizer(Player player, ItemStack currentStack) {
        ItemStack best = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                if (best.isEmpty() || stack.getDamageValue() < best.getDamageValue()) {
                    best = stack;
                }
            }
        }
        return best == currentStack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                CompoundTag nbt = stack.getOrCreateTag();
                int mode = nbt.getInt("Mode");
                int currentE = pEnergy.getEnergy();
                int maxE = pEnergy.getMaxEnergy();

                // 1. ПЕРЕВІРКА ПРІОРИТЕТІВ
                boolean externalActive = pEnergy.isExternalStabilizationActive();
                pEnergy.setRemoteNoDrain(true);

                // 2. АВТОМАТИЧНА СТАБІЛІЗАЦІЯ
                if (isBestStabilizer(player, stack) && !externalActive) {
                    int thresholdPercent = getThresholdForMode(mode);
                    int thresholdValue = (maxE * thresholdPercent) / 100;

                    if (currentE > thresholdValue) {
                        int excess = currentE - thresholdValue;
                        pEnergy.extractEnergyPure(excess, false);

                        // Викидаємо енергію в світ через івент (без поглинання предметом)
                        MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, player.blockPosition(), excess));

                        // Знос
                        int damage = Math.max(1, excess / 300);
                        if (level.getGameTime() % 15 == 0) {
                            stack.hurtAndBreak(damage, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.05f, 2.0f);
                        }
                    }
                }

                // 3. АВТО-РЕМОНТ (тільки з буфера, який ти зарядив у блоках)
                int internalE = nbt.getInt("TachyonBuffer");
                if (stack.isDamaged() && internalE >= 50) {
                    if (level.getGameTime() % 80 == 0) {
                        nbt.putInt("TachyonBuffer", internalE - 50);
                        stack.setDamageValue(stack.getDamageValue() - 1);
                    }
                }

                int threshold = (maxE * getThresholdForMode(mode)) / 100;
                pEnergy.setRegenBlocked(mode != 3 && currentE >= threshold && !externalActive);
            });
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int mode = (stack.getOrCreateTag().getInt("Mode") + 1) % 4;
                stack.getOrCreateTag().putInt("Mode", mode);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MODE_SWITCH.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
                player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName(mode)), true);
            }
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer sPlayer) {
            sPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                if (pEnergy.getEnergy() > 10) {
                    int toDrain = pEnergy.getMaxEnergy() / 20;
                    pEnergy.extractEnergyPure(toDrain, false);

                    // Викидаємо енергію через івент при ручному скиданні
                    MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, player.blockPosition(), toDrain));

                    stack.hurtAndBreak(5, sPlayer, (p) -> p.broadcastBreakEvent(hand));

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4f, 2.0f);

                    ((ServerLevel)level).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1, player.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
                }
            });
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    public static boolean tryPreventCollapse(ServerPlayer player, PlayerEnergy energy) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                stack.hurtAndBreak(250, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 0.5f);

                ((ServerLevel)player.level()).sendParticles(ParticleTypes.FLASH,
                        player.getX(), player.getY() + 1, player.getZ(), 2, 0.2, 0.2, 0.2, 0);

                energy.setEnergy((int)(energy.getMaxEnergy() * 1.90f));
                player.displayClientMessage(Component.translatable("message.tacionian.safety_limit").withStyle(net.minecraft.ChatFormatting.RED), true);
                return true;
            }
        }
        return false;
    }

    public static int getThresholdForMode(int mode) {
        return switch (mode) {
            case 0 -> 75;  // Safe
            case 1 -> 40;  // Balanced
            case 2 -> 15;  // Performance
            default -> 200; // Unrestricted
        };
    }

    private Component getModeName(int mode) {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe");
            case 1 -> Component.translatable("mode.tacionian.balanced");
            case 2 -> Component.translatable("mode.tacionian.performance");
            default -> Component.translatable("mode.tacionian.unrestricted");
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = stack.getOrCreateTag();
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc"));
        tooltip.add(Component.translatable("command.tacionian.info.energy", nbt.getInt("TachyonBuffer"), MAX_INTERNAL_ENERGY));

        float durPct = ((float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()) * 100;
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.integrity").append(String.format(": %.0f%%", durPct)));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.mode").append(getModeName(nbt.getInt("Mode"))));
    }
}