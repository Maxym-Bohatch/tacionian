package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModCapabilities;
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

        if (level.getGameTime() % 5 == 0) {
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int minEnergy = (int) (pEnergy.getMaxEnergy() * 0.15f);
                // Використовуємо масив, щоб Java дозволила змінювати значення всередині лямбда-виразів
                final int[] availableTx = { pEnergy.getEnergy() - minEnergy };

                if (availableTx[0] <= 0) return;

                boolean changed = false;
                for (ItemStack target : serverPlayer.getInventory().items) {
                    if (target.isEmpty() || target == stack) continue;

                    // 1. Пріоритет: Tachyon (Твій мод)
                    var txCap = target.getCapability(ModCapabilities.TACHYON_STORAGE);
                    if (txCap.isPresent()) {
                        int taken = txCap.map(cap -> {
                            int needed = cap.getMaxCapacity() - cap.getEnergy();
                            int toGive = Math.min(availableTx[0], Math.min(needed, 50));
                            int extracted = pEnergy.extractEnergyWithExp(toGive, false, serverPlayer);
                            return cap.receiveTacionEnergy(extracted, false);
                        }).orElse(0);

                        if (taken > 0) {
                            changed = true;
                            availableTx[0] -= taken;
                            if (availableTx[0] <= 0) break;
                            continue;
                        }
                    }

                    // 2. RF Енергія (Інші моди)
                    var rfCap = target.getCapability(ForgeCapabilities.ENERGY);
                    if (rfCap.isPresent()) {
                        int taken = rfCap.map(cap -> {
                            if (!cap.canReceive()) return 0;
                            int neededRF = Math.min(cap.receiveEnergy(500, true), availableTx[0] * 10);
                            if (neededRF > 0) {
                                int txToTake = (neededRF + 9) / 10;
                                int extracted = pEnergy.extractEnergyWithExp(txToTake, false, serverPlayer);
                                return cap.receiveEnergy(extracted * 10, false);
                            }
                            return 0;
                        }).orElse(0);

                        if (taken > 0) {
                            changed = true;
                            availableTx[0] -= (taken / 10);
                            if (availableTx[0] <= 0) break;
                        }
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