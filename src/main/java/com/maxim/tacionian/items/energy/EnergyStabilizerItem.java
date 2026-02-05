/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.ITachyonChargeable; // ВИПРАВЛЕНО
import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModCapabilities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyStabilizerItem extends Item implements ITachyonChargeable {
    public static final int MAX_INTERNAL_ENERGY = 10000;

    public EnergyStabilizerItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.RARE).durability(1000));
    }

    // --- РЕАЛІЗАЦІЯ ITachyonChargeable ---
    @Override public int getMaxEnergy(ItemStack stack) { return MAX_INTERNAL_ENERGY; }
    @Override public int getEnergy(ItemStack stack) { return stack.getOrCreateTag().getInt("TachyonBuffer"); }

    @Override
    public int receiveEnergy(ItemStack stack, int amount, boolean simulate) {
        int stored = getEnergy(stack);
        int canReceive = Math.min(MAX_INTERNAL_ENERGY - stored, amount);
        if (!simulate && canReceive > 0) {
            stack.getOrCreateTag().putInt("TachyonBuffer", stored + canReceive);
        }
        return canReceive;
    }

    @Override
    public int extractEnergy(ItemStack stack, int amount, boolean simulate) {
        int stored = getEnergy(stack);
        int canExtract = Math.min(stored, amount);
        if (!simulate && canExtract > 0) {
            stack.getOrCreateTag().putInt("TachyonBuffer", stored - canExtract);
        }
        return canExtract;
    }

    // --- CAPABILITY ---
    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<ITachyonStorage> handler = LazyOptional.of(() -> new ITachyonStorage() {
                @Override public int receiveTacionEnergy(int amount, boolean simulate) { return receiveEnergy(stack, amount, simulate); }
                @Override public int extractTacionEnergy(int amount, boolean simulate) { return extractEnergy(stack, amount, simulate); }
                @Override public int getEnergy() { return EnergyStabilizerItem.this.getEnergy(stack); }
                @Override public int getMaxCapacity() { return MAX_INTERNAL_ENERGY; }
            });

            @Override
            public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
                return cap == ModCapabilities.TACHYON_STORAGE ? handler.cast() : LazyOptional.empty();
            }
        };
    }

    // Метод для запобігання вибуху
    public static boolean tryPreventCollapse(ServerPlayer player, PlayerEnergy energy) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                stack.hurtAndBreak(250, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 0.5f);
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1, player.getZ(), 2, 0.2, 0.2, 0.2, 0);
                }
                energy.setEnergy((int)(energy.getMaxEnergy() * 1.90f));
                player.displayClientMessage(Component.translatable("message.tacionian.safety_limit").withStyle(net.minecraft.ChatFormatting.RED), true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int mode = stack.getOrCreateTag().getInt("Mode");
                int currentE = pEnergy.getEnergy();
                pEnergy.setRemoteNoDrain(true);

                // Авто-стабілізація
                if (isBestStabilizer(player, stack) && !pEnergy.isExternalStabilizationActive()) {
                    int threshold = (pEnergy.getMaxEnergy() * getThresholdForMode(mode)) / 100;
                    if (currentE > threshold) {
                        int excess = currentE - threshold;
                        pEnergy.extractEnergyPure(excess, false);
                        MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, player.blockPosition(), excess));
                        if (level.getGameTime() % 15 == 0) {
                            stack.hurtAndBreak(Math.max(1, excess / 300), player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.05f, 2.0f);
                        }
                    }
                }

                // Авто-ремонт
                if (stack.isDamaged() && getEnergy(stack) >= 50 && level.getGameTime() % 80 == 0) {
                    extractEnergy(stack, 50, false);
                    stack.setDamageValue(stack.getDamageValue() - 1);
                }
                pEnergy.setRegenBlocked(mode != 3 && currentE >= (pEnergy.getMaxEnergy() * getThresholdForMode(mode)) / 100 && !pEnergy.isExternalStabilizationActive());
            });
        }
    }

    private boolean isBestStabilizer(Player player, ItemStack currentStack) {
        ItemStack best = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof EnergyStabilizerItem) {
                if (best.isEmpty() || stack.getDamageValue() < best.getDamageValue()) best = stack;
            }
        }
        return best == currentStack;
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
                    MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, player.blockPosition(), toDrain));
                    stack.hurtAndBreak(5, sPlayer, (p) -> p.broadcastBreakEvent(hand));
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.4f, 2.0f);
                    ((ServerLevel)level).sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
                }
            });
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    public static int getThresholdForMode(int mode) { return switch (mode) { case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 200; }; }
    private Component getModeName(int mode) { return switch (mode) { case 0 -> Component.translatable("mode.tacionian.safe"); case 1 -> Component.translatable("mode.tacionian.balanced"); case 2 -> Component.translatable("mode.tacionian.performance"); default -> Component.translatable("mode.tacionian.unrestricted"); }; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc"));
        tooltip.add(Component.translatable("command.tacionian.info.energy", getEnergy(stack), MAX_INTERNAL_ENERGY));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.integrity").append(String.format(": %.0f%%", (1.0f - (float)stack.getDamageValue()/stack.getMaxDamage()) * 100)));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.mode").append(getModeName(stack.getOrCreateTag().getInt("Mode"))));
    }
}