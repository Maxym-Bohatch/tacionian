package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BasicChargerItem extends Item {
    public BasicChargerItem(Properties props) { super(props.stacksTo(1)); }

    // Перемикач режимів на ПКМ
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean active = !stack.getOrCreateTag().getBoolean("Active");
            stack.getOrCreateTag().putBoolean("Active", active);

            player.displayClientMessage(Component.translatable(active ? "message.tacionian.activated" : "message.tacionian.deactivated"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // Пасивна робота в інвентарі
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || !stack.getOrCreateTag().getBoolean("Active")) return;

        // Кожні 20 тиків (1 секунда) проводимо зарядку, щоб не навантажувати сервер
        if (level.getGameTime() % 20 == 0) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                if (pEnergy.getEnergy() <= 0) return;

                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack target = player.getInventory().getItem(i);
                    if (target.isEmpty() || target == stack) continue;

                    target.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
                        if (cap.canReceive()) {
                            int toTransfer = Math.min(pEnergy.getEnergy(), 500); // По 500 RF за раз
                            int accepted = cap.receiveEnergy(toTransfer, true);
                            if (accepted > 0) {
                                int extracted = pEnergy.extractEnergyWithExp(accepted, false, level.getGameTime(), player);
                                cap.receiveEnergy(extracted, false);
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) { return stack.getOrCreateTag().getBoolean("Active"); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean active = stack.getOrCreateTag().getBoolean("Active");
        tooltip.add(Component.translatable(active ? "tooltip.tacionian.active" : "tooltip.tacionian.inactive")
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}