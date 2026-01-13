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

public class SafeChargerItem extends Item {
    public SafeChargerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean active = !stack.getOrCreateTag().getBoolean("Active");
            stack.getOrCreateTag().putBoolean("Active", active);
            // Використовуємо ключі локалізації, які ми прописали раніше
            player.displayClientMessage(Component.translatable(active ? "tooltip.tacionian.active" : "tooltip.tacionian.inactive"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || !stack.getOrCreateTag().getBoolean("Active")) return;

        // Кожну секунду (20 тіків)
        if (level.getGameTime() % 20 == 0) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // Перевірка безпеки (залишаємо мінімум 15% енергії гравцю)
                int minEnergy = (int) (pEnergy.getMaxEnergy() * 0.15f);
                int availableTx = pEnergy.getEnergy() - minEnergy;

                if (availableTx <= 0) return;

                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack target = player.getInventory().getItem(i);
                    if (target.isEmpty() || target == stack) continue;

                    target.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
                        // Конвертація: 1 Tx = 10 RF (якщо accepted повертає RF)
                        int maxRfToGive = Math.min(availableTx * 10, 1000);
                        int acceptedRf = cap.receiveEnergy(maxRfToGive, true);

                        if (acceptedRf > 0) {
                            int txToExtract = acceptedRf / 10;
                            if (txToExtract < 1) txToExtract = 1;

                            // ВИПРАВЛЕНО: Видалено level.getGameTime()
                            int extractedTx = pEnergy.extractEnergyWithExp(txToExtract, false, player);
                            cap.receiveEnergy(extractedTx * 10, false);
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
        tooltip.add(Component.translatable("item.tacionian.safe_charger_item").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(active ? "tooltip.tacionian.active" : "tooltip.tacionian.inactive")
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}