package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

public class SafeChargerItem extends Item {
    public SafeChargerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean active = !stack.getOrCreateTag().getBoolean("Active");
            stack.getOrCreateTag().putBoolean("Active", active);
            player.displayClientMessage(Component.translatable(active ? "tooltip.tacionian.active" : "tooltip.tacionian.inactive"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer) || !stack.getOrCreateTag().getBoolean("Active")) return;

        // Зменшуємо інтервал до 10 тіків для більш плавного нарахування досвіду
        if (level.getGameTime() % 10 == 0) {
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int minEnergy = (int) (pEnergy.getMaxEnergy() * 0.15f);
                int availableTx = pEnergy.getEnergy() - minEnergy;
                if (availableTx <= 0) return;

                boolean changed = false;
                for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                    ItemStack target = serverPlayer.getInventory().getItem(i);
                    if (target.isEmpty() || target == stack) continue;

                    if (target.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                        final boolean[] success = {false};
                        target.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
                            int maxRfToGive = Math.min(availableTx * 10, 1000);
                            int acceptedRf = cap.receiveEnergy(maxRfToGive, true);
                            if (acceptedRf > 0) {
                                // Гарантуємо мінімум 1 Tx для активації логіки досвіду
                                int txToExtract = Math.max(1, acceptedRf / 10);
                                int extractedTx = pEnergy.extractEnergyWithExp(txToExtract, false, serverPlayer);

                                if (extractedTx > 0) {
                                    cap.receiveEnergy(extractedTx * 10, false);
                                    success[0] = true;
                                }
                            }
                        });
                        if (success[0]) changed = true;
                    }
                }
                if (changed) pEnergy.sync(serverPlayer);
            });
        }
    }

    @Override public boolean isFoil(ItemStack stack) { return stack.getOrCreateTag().getBoolean("Active"); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean active = stack.getOrCreateTag().getBoolean("Active");
        tooltip.add(Component.translatable("item.tacionian.safe_charger_item").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tacionian.safe_charger_item.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(active ? "tooltip.tacionian.active" : "tooltip.tacionian.inactive")
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}