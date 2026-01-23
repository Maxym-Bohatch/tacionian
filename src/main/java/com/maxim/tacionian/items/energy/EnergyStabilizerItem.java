package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.world.entity.Entity;

public class EnergyStabilizerItem extends Item {
    public EnergyStabilizerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int mode = stack.getOrCreateTag().getInt("Mode");
                int threshold = (pEnergy.getMaxEnergy() * getThresholdForMode(mode)) / 100;

                // Блокуємо реген, тільки якщо енергія ВИЩЕ або ДОРІВНЮЄ порогу режиму
                if (pEnergy.getEnergy() >= threshold) {
                    pEnergy.setRemoteNoDrain(true);
                }

                if (level.getGameTime() % 10 == 0) pEnergy.sync(player);
            });
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int mode = (stack.getOrCreateTag().getInt("Mode") + 1) % 4;
            stack.getOrCreateTag().putInt("Mode", mode);
            level.playSound(null, player.blockPosition(), ModSounds.MODE_SWITCH.get(), SoundSource.PLAYERS, 0.5f, 0.8f + (mode * 0.2f));
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
                int mode = stack.getOrCreateTag().getInt("Mode");
                int thresholdValue = (pEnergy.getMaxEnergy() * getThresholdForMode(mode)) / 100;

                if (pEnergy.getEnergy() > thresholdValue && player.tickCount % 10 == 0) {
                    player.getActiveEffects().stream()
                            .filter(e -> !e.getEffect().isBeneficial())
                            .findFirst()
                            .ifPresent(effect -> {
                                player.removeEffect(effect.getEffect());
                                pEnergy.extractEnergyPure(50, false);
                                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                        net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.5f, 2.0f);
                            });
                }

                if (pEnergy.getEnergy() > thresholdValue) {
                    int excess = pEnergy.getEnergy() - thresholdValue;
                    int toDrain = Math.min(excess, 30);
                    pEnergy.extractEnergyPure(toDrain, false);
                    pEnergy.setRemoteNoDrain(true);

                    if (count % 5 == 0) {
                        ((ServerLevel)level).sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.15f, 1.5f);
                    }
                    pEnergy.sync(player);
                }
            });
        }
    }

    private int getThresholdForMode(int mode) {
        return switch (mode) { case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 0; };
    }

    private Component getModeName(int mode) {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe").withStyle(ChatFormatting.GREEN);
            case 1 -> Component.translatable("mode.tacionian.balanced").withStyle(ChatFormatting.YELLOW);
            case 2 -> Component.translatable("mode.tacionian.performance").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("mode.tacionian.unrestricted").withStyle(ChatFormatting.RED);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int mode = stack.hasTag() ? stack.getTag().getInt("Mode") : 0;
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.mode").append(": ").append(getModeName(mode)));
    }

    @Override public int getUseDuration(ItemStack s) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
}