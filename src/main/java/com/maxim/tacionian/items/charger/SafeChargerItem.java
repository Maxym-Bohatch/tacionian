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
            player.displayClientMessage(Component.translatable(active ? "status.tacionian.active" : "status.tacionian.disabled"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) return;
        if (!stack.getOrCreateTag().getBoolean("Active")) return;

        if (level.getGameTime() % 5 == 0) {
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int minEnergy = (int) (pEnergy.getMaxEnergy() * 0.15f); // Поріг 15%
                int currentEnergy = pEnergy.getEnergy();

                if (currentEnergy <= minEnergy) return;

                final int[] availableToTake = { currentEnergy - minEnergy };
                boolean changed = false;

                for (ItemStack target : serverPlayer.getInventory().items) {
                    if (target.isEmpty() || target == stack) continue;
                    if (availableToTake[0] <= 0) break;

                    var txCapOpt = target.getCapability(ModCapabilities.TACHYON_STORAGE);
                    if (txCapOpt.isPresent()) {
                        int txAdded = txCapOpt.map(cap -> {
                            int needed = cap.getMaxCapacity() - cap.getEnergy();
                            if (needed <= 0) return 0;
                            int toGive = Math.min(availableToTake[0], Math.min(needed, 50));
                            int extracted = pEnergy.extractEnergyPure(toGive, false);
                            if (extracted > 0) {
                                pEnergy.addExperience(extracted * 0.12f, serverPlayer);
                                return cap.receiveTacionEnergy(extracted, false);
                            }
                            return 0;
                        }).orElse(0);

                        if (txAdded > 0) {
                            changed = true;
                            availableToTake[0] -= txAdded;
                            continue;
                        }
                    }

                    var rfCapOpt = target.getCapability(ForgeCapabilities.ENERGY);
                    if (rfCapOpt.isPresent()) {
                        int rfAdded = rfCapOpt.map(cap -> {
                            if (!cap.canReceive()) return 0;
                            int maxRF = 500;
                            int canAcceptRF = cap.receiveEnergy(maxRF, true);
                            if (canAcceptRF > 0) {
                                int txNeeded = (int) Math.ceil(canAcceptRF / 10.0);
                                int toGive = Math.min(availableToTake[0], Math.min(txNeeded, 50));
                                int extracted = pEnergy.extractEnergyPure(toGive, false);
                                if (extracted > 0) {
                                    pEnergy.addExperience(extracted * 0.18f, serverPlayer);
                                    return cap.receiveEnergy(extracted * 10, false);
                                }
                            }
                            return 0;
                        }).orElse(0);

                        if (rfAdded > 0) {
                            changed = true;
                            availableToTake[0] -= (rfAdded / 10);
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

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean("Active");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean active = stack.getOrCreateTag().getBoolean("Active");
        // Передаємо 15% у локалізацію
        tooltip.add(Component.translatable("tooltip.tacionian.safe_charger_item.desc", 15).withStyle(ChatFormatting.GRAY));

        String statusKey = active ? "status.tacionian.active" : "status.tacionian.disabled";
        tooltip.add(Component.translatable(statusKey)
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}