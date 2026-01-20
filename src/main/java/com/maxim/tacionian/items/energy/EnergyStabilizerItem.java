package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyStabilizerItem extends Item {
    public EnergyStabilizerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int mode = (stack.getOrCreateTag().getInt("Mode") + 1) % 4;
            stack.getOrCreateTag().putInt("Mode", mode);
            // ВЛАСНИЙ ЗВУК: Перемикання режиму
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
                int threshold = getThresholdForMode(mode);

                if (pEnergy.getEnergyPercent() > threshold) {
                    int extracted = pEnergy.extractEnergyPure(50, false);
                    if (extracted > 0) {
                        // ВЛАСНИЙ ЗВУК: Гудіння при скиданні енергії
                        if (count % 10 == 0) {
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TACHYON_HUM.get(), SoundSource.PLAYERS, 0.2f, 1.5f);
                        }
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                                new com.maxim.tacionian.api.events.TachyonWasteEvent(level, player.blockPosition(), extracted)
                        );
                    }
                    if (count % 5 == 0) pEnergy.sync(player);
                }
            });
        }
    }

    private int getThresholdForMode(int mode) {
        return switch (mode) { case 0 -> 75; case 1 -> 40; case 2 -> 15; default -> 0; };
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
        int mode = stack.getOrCreateTag().getInt("Mode");
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.mode").append(getModeName(mode)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.use_info").withStyle(ChatFormatting.YELLOW));
    }

    @Override public int getUseDuration(ItemStack s) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
}