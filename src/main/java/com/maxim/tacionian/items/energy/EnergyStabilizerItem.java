package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    public EnergyStabilizerItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift + ПКМ: Перемикання режимів
        if (player.isShiftKeyDown()) {
            int mode = (stack.getOrCreateTag().getInt("Mode") + 1) % 4;
            stack.getOrCreateTag().putInt("Mode", mode);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.tacionian.mode_switched", getModeName(mode)), true);
            }
            return InteractionResultHolder.success(stack);
        }

        // Просто ПКМ: Починаємо затискати (використовувати)
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // ЛОГІКА В ІНВЕНТАРІ (Працює пасивно)
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int mode = stack.getOrCreateTag().getInt("Mode");
                int threshold = getThresholdForMode(mode);

                // Якщо енергія вища за поріг — блокуємо регенерацію ядра
                if (pEnergy.getEnergyPercent() > threshold) {
                    pEnergy.setRemoteNoDrain(true);
                }
            });
        }
    }

    // ЛОГІКА ПРИ ЗАЖАТТІ ПКМ (Активний злив)
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int mode = stack.getOrCreateTag().getInt("Mode");
                int threshold = getThresholdForMode(mode);

                // Зливаємо енергію тільки якщо вона вища за поріг
                if (pEnergy.getEnergyPercent() > threshold) {
                    // Зливаємо 50 одиниць за кожен "тік" використання
                    pEnergy.extractEnergyPure(50, false);

                    // Синхронізуємо HUD кожні 5 тіків, щоб не спамити пакетами
                    if (count % 5 == 0) pEnergy.sync(player);
                }
            });
        }
    }

    private int getThresholdForMode(int mode) {
        return switch (mode) {
            case 0 -> 75; // Safe
            case 1 -> 40; // Balanced
            case 2 -> 15; // Performance
            default -> 0; // Unrestricted
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
        int mode = stack.getOrCreateTag().getInt("Mode");

        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc").withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.mode")
                .append(getModeName(mode)).withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.use_info").withStyle(ChatFormatting.YELLOW));
    }

    @Override public int getUseDuration(ItemStack s) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
}