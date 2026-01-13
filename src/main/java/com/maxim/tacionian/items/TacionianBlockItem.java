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

        // 1. Бездротовий інтерфейс
        if (block == ModBlocks.WIRELESS_ENERGY_INTERFACE.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.wireless_interface.desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.tacionian.wireless_interface.power").withStyle(ChatFormatting.GRAY));
        }
        // 2. Стабілізаційна плита
        else if (block == ModBlocks.STABILIZATION_PLATE.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.stabilization_plate.desc").withStyle(ChatFormatting.GREEN));
        }
        // 3. Блок безпечної зарядки (додано)
        else if (block == ModBlocks.SAFE_CHARGER_BLOCK.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.safe_charger_block.desc").withStyle(ChatFormatting.YELLOW));
        }
        // 4. Резервуар енергії
        else if (block == ModBlocks.ENERGY_RESERVOIR.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.energy_reservoir.desc").withStyle(ChatFormatting.BLUE));
        }
        // 5. Базовий блок (якщо такий є)
        else if (block == ModBlocks.BASIC_CHARGER_BLOCK.get()) {
            tooltip.add(Component.translatable("tooltip.tacionian.basic_charger_block.desc").withStyle(ChatFormatting.GRAY));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}