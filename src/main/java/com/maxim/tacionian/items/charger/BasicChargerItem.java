package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BasicChargerItem extends Item {
    public BasicChargerItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) return;

        serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            // Використовуємо масив для стабільності, хоча тут це не обов'язково
            final int[] availableTx = { pEnergy.getEnergy() };
            if (availableTx[0] <= 0) return;

            boolean changed = false;
            for (ItemStack target : serverPlayer.getInventory().items) {
                if (target.isEmpty() || target == stack) continue;

                // 1. Пріоритет: Tachyon (Твій мод)
                var txCapOpt = target.getCapability(ModCapabilities.TACHYON_STORAGE);
                if (txCapOpt.isPresent()) {
                    int taken = txCapOpt.map(cap -> {
                        int needed = cap.getMaxCapacity() - cap.getEnergy();
                        int toGive = Math.min(availableTx[0], Math.min(needed, 10)); // 10 Tx за тік
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

                // 2. Другорядне: RF (Інші моди)
                var rfCapOpt = target.getCapability(ForgeCapabilities.ENERGY);
                if (rfCapOpt.isPresent()) {
                    int taken = rfCapOpt.map(cap -> {
                        if (!cap.canReceive()) return 0;
                        int neededRF = Math.min(cap.receiveEnergy(100, true), availableTx[0] * 10);
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
                if (level.getGameTime() % 10 == 0 && level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(),
                            2, 0.3, 0.3, 0.3, 0.05);
                }
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tacionian.basic_charger_item.desc").withStyle(ChatFormatting.GRAY));
    }
}