package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyCellItem extends Item {
    private final int capacity = 3000;

    public EnergyCellItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            CompoundTag nbt = stack.getOrCreateTag();
            int current = nbt.getInt("energy");

            if (player.isShiftKeyDown()) {
                // Віддаємо енергію з ячейки в ядро Макса (Стабілізація)
                int toGive = Math.min(Math.min(current, 100), pEnergy.getMaxEnergy() - pEnergy.getEnergy());
                if (toGive > 0) {
                    pEnergy.receiveEnergy(toGive, false);
                    nbt.putInt("energy", current - toGive);
                }
            } else {
                // Забираємо надлишок з ядра в ячейку (Скид енергії)
                int toTake = Math.min(Math.min(pEnergy.getEnergy(), 100), capacity - current);
                if (toTake > 0) {
                    pEnergy.extractEnergyPure(toTake, false, level.getGameTime());
                    nbt.putInt("energy", current + toTake);
                }
            }
        });
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int current = stack.hasTag() ? stack.getTag().getInt("energy") : 0;

        // Опис призначення
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        // Показник заряду в Тахіонах (Tx)
        tooltip.add(Component.literal("§eЗапас: " + current + " / " + capacity + " Tx"));

        // Підказки по управлінню
        tooltip.add(Component.literal("§8ПКМ: Забрати надлишок з ядра"));
        tooltip.add(Component.literal("§8Shift+ПКМ: Підживити ядро"));
    }

    // Робимо ячейку візуально зарядженою (енchant effect), якщо в ній більше 0 енергії
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt("energy") > 0;
    }
}