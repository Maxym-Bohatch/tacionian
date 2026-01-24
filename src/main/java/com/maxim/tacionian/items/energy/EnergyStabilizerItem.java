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
    public EnergyStabilizerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // Предмет у кишені дає знати HUD, що стабілізація підключена
                pEnergy.setRemoteNoDrain(true);

                int mode = stack.getOrCreateTag().getInt("Mode");

                // ГОЛОВНА ЗМІНА:
                // Якщо режим 3, ми НЕ блокуємо регенерацію. Гравець може вільно заряджатися до 200%.
                // В інших режимах (0,1,2) предмет працює як пасивний обмежувач.
                pEnergy.setRegenBlocked(mode != 3);

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
                int mode = stack.getOrCreateTag().getInt("Mode");

                // ПОРІГ: В 3-му режимі при затисканні ПКМ поріг стає 0 (екстрений злив усього)
                int thresholdValue = (mode == 3) ? 0 : (pEnergy.getMaxEnergy() * getThresholdForMode(mode)) / 100;

                if (pEnergy.getEnergy() > thresholdValue) {
                    // Очищення дебафів
                    if (player.tickCount % 10 == 0) {
                        player.getActiveEffects().stream().filter(e -> !e.getEffect().isBeneficial()).findFirst().ifPresent(effect -> {
                            player.removeEffect(effect.getEffect());
                            pEnergy.extractEnergyPure(150, false);
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.6f, 1.2f);
                        });
                    }

                    // Злиття енергії
                    int excess = pEnergy.getEnergy() - thresholdValue;
                    // В Unrestricted (3) злиття йде швидше (екстрена ситуація)
                    int toDrain = (mode == 3) ? 100 + (excess / 6) : 50 + (excess / 8);

                    pEnergy.extractEnergyPure(toDrain, false);
                    MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, player.blockPosition(), toDrain));

                    if (count % 5 == 0) {
                        float pitch = (mode == 3) ? 0.9f : 1.4f;
                        level.playSound(null, player.blockPosition(), ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.2f, pitch);

                        // Спецефекти
                        var particle = (mode == 3) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ELECTRIC_SPARK;
                        ((ServerLevel)level).sendParticles(particle, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                    }
                    pEnergy.sync(player);
                }
            });
        }
    }

    public static int getThresholdForMode(int mode) {
        return switch (mode) { case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 0; };
    }

    private Component getModeName(int mode) {
        return switch (mode) {
            case 0 -> Component.translatable("mode.tacionian.safe").withStyle(ChatFormatting.GREEN);
            case 1 -> Component.translatable("mode.tacionian.balanced").withStyle(ChatFormatting.YELLOW);
            case 2 -> Component.translatable("mode.tacionian.performance").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("mode.tacionian.unrestricted").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        };
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int mode = stack.hasTag() ? stack.getTag().getInt("Mode") : 0;
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.mode").append(": ").append(getModeName(mode)));
    }

    @Override public int getUseDuration(ItemStack s) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
}