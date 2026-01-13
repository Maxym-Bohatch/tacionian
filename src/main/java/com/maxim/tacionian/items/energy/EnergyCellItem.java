package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    public EnergyCellItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);

        serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            CompoundTag nbt = stack.getOrCreateTag();
            int stored = nbt.getInt("energy");
            int amount = 100;

            if (serverPlayer.isShiftKeyDown()) {
                int toGive = Math.min(Math.min(stored, amount), pEnergy.getMaxEnergy() - pEnergy.getEnergy());
                if (toGive > 0) {
                    pEnergy.receiveEnergy(toGive, false);
                    nbt.putInt("energy", stored - toGive);
                }
            } else {
                int spaceInCell = 3000 - stored;
                int toTake = Math.min(Math.min(pEnergy.getEnergy(), amount), spaceInCell);
                if (toTake > 0) {
                    pEnergy.extractEnergyPure(toTake, false);
                    nbt.putInt("energy", stored + toTake);
                }
            }
            pEnergy.sync(serverPlayer);
        });
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int energy = stack.hasTag() ? stack.getTag().getInt("energy") : 0;
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.charge", energy, 3000).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.controls_1").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.controls_2").withStyle(ChatFormatting.DARK_GRAY));
    }
}