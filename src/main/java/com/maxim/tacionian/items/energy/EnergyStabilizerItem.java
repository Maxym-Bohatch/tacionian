package com.maxim.tacionian.items.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyStabilizerItem extends Item {
    public EnergyStabilizerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ПЕРЕМИКАННЯ РЕЖИМІВ (Shift + ПКМ)
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag nbt = stack.getOrCreateTag();
                int mode = (nbt.getInt("StabilizationMode") + 1) % 3; // 0: 30%, 1: 60%, 2: 90%
                nbt.putInt("StabilizationMode", mode);

                String percent = mode == 0 ? "30%" : mode == 1 ? "60%" : "90%";
                player.sendSystemMessage(Component.literal("§b[Стабілізатор] §7Поріг встановлено на: §f" + percent));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }
    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tacionian.energy_stabilizer.desc"));

        // Відображення поточного режиму з NBT
        int mode = stack.hasTag() ? stack.getTag().getInt("StabilizationMode") : 0;
        String percent = mode == 0 ? "30%" : mode == 1 ? "60%" : "90%";

        tooltip.add(Component.translatable("tooltip.tacionian.mode_info", percent));
        tooltip.add(Component.translatable("tooltip.tacionian.control_hint"));
    }
}