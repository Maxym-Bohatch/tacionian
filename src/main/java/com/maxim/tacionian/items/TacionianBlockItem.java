package com.maxim.tacionian.items;

import com.maxim.tacionian.register.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TacionianBlockItem extends BlockItem {
    public TacionianBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Block block = this.getBlock();

        if (block == ModBlocks.WIRELESS_ENERGY_INTERFACE.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.wireless_interface.desc").withStyle(ChatFormatting.AQUA));
            // Передаємо радіус 20
            tooltip.add(Component.translatable("tooltip.tacionian.wireless_interface.power", 20).withStyle(ChatFormatting.YELLOW));
        }
        else if (block == ModBlocks.STABILIZATION_PLATE.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.stabilization_plate.desc").withStyle(ChatFormatting.GREEN));
        }
        else if (block == ModBlocks.SAFE_CHARGER_BLOCK.get()) {
            // Передаємо ліміт 20%
            tooltip.add(Component.translatable("tooltip.tacionian.safe_charger_block.desc", 20).withStyle(ChatFormatting.YELLOW));
        }
        else if (block == ModBlocks.ENERGY_RESERVOIR.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.energy_reservoir.desc").withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("tooltip.tacionian.energy_reservoir.unstable_note").withStyle(ChatFormatting.DARK_PURPLE));
        }
        else if (block == ModBlocks.BASIC_CHARGER_BLOCK.get()) {
            // Передаємо швидкість 500
            tooltip.add(Component.translatable("tooltip.tacionian.basic_charger_block.desc", 500).withStyle(ChatFormatting.GRAY));
        }
        else if (block == ModBlocks.TACHYON_CABLE.get()) {
            // Передаємо пропускну здатність 1000
            tooltip.add(Component.translatable("tooltip.tacionian.tachyon_cable.desc", 1000).withStyle(ChatFormatting.GRAY));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}