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

        if (block == ModBlocks.WIRELESS_INTERFACE.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.wireless_interface.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.tacionian.control_hint").withStyle(ChatFormatting.GRAY));
        }
        else if (block == ModBlocks.STABILIZATION_PLATE.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.stabilization_plate.desc").withStyle(ChatFormatting.GREEN));
        }
        else if (block == ModBlocks.BASIC_CHARGER_BLOCK.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.basic_charger_block.desc").withStyle(ChatFormatting.GRAY));
        }
        else if (block == ModBlocks.SAFE_CHARGER_BLOCK.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.safe_charger_block.desc").withStyle(ChatFormatting.GOLD));
        }
        // Додано для резервуара
        else if (block == ModBlocks.RESERVOIR_BLOCK.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.reservoir.desc").withStyle(ChatFormatting.BLUE));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}