package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) return;
        if (!stack.getOrCreateTag().getBoolean("Active")) return;

        // Обробка кожні 5 тіків для балансу продуктивності та досвіду
        if (level.getGameTime() % 5 == 0) {
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // Безпечний поріг: 15% енергії залишається в ядрі
                int minEnergy = (int) (pEnergy.getMaxEnergy() * 0.15f);
                int availableTx = pEnergy.getEnergy() - minEnergy;
                if (availableTx <= 0) return;

                boolean changed = false;
                for (ItemStack target : serverPlayer.getInventory().items) {
                    if (target.isEmpty() || target == stack) continue;

                    var capOpt = target.getCapability(ForgeCapabilities.ENERGY);
                    if (capOpt.isPresent()) {
                        final boolean[] success = {false};
                        capOpt.ifPresent(cap -> {
                            if (cap.canReceive()) {
                                int maxRfToGive = Math.min(availableTx * 10, 1000);
                                int acceptedRf = cap.receiveEnergy(maxRfToGive, true);

                                if (acceptedRf > 0) {
                                    int txToExtract = (acceptedRf + 9) / 10;
                                    int extractedTx = pEnergy.extractEnergyWithExp(txToExtract, false, serverPlayer);

                                    if (extractedTx > 0) {
                                        cap.receiveEnergy(extractedTx * 10, false);
                                        success[0] = true;
                                    }
                                }
                            }
                        });
                        if (success[0]) changed = true;
                    }
                }

                if (changed) {
                    pEnergy.sync(serverPlayer);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                                serverPlayer.getX(), serverPlayer.getY() + 1.2, serverPlayer.getZ(),
                                1, 0.2, 0.2, 0.2, 0.02);
                    }
                }
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