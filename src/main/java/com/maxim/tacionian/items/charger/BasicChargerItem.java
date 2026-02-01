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
            int availableTx = pEnergy.getEnergy();
            if (availableTx <= 0) return;

            boolean changed = false;
            for (ItemStack target : serverPlayer.getInventory().items) {
                if (target.isEmpty() || target == stack) continue;

                var txCapOpt = target.getCapability(ModCapabilities.TACHYON_STORAGE);
                if (txCapOpt.isPresent()) {
                    int taken = txCapOpt.map(cap -> {
                        int needed = cap.getMaxCapacity() - cap.getEnergy();
                        int toGive = Math.min(pEnergy.getEnergy(), Math.min(needed, 10)); // Швидкість 10

                        int extracted = pEnergy.extractEnergyPure(toGive, false);
                        if (extracted > 0) {
                            pEnergy.addExperience(extracted * 0.1f, serverPlayer);
                            return cap.receiveTacionEnergy(extracted, false);
                        }
                        return 0;
                    }).orElse(0);

                    if (taken > 0) {
                        changed = true;
                        if (pEnergy.getEnergy() <= 0) break;
                        continue;
                    }
                }

                var rfCapOpt = target.getCapability(ForgeCapabilities.ENERGY);
                if (rfCapOpt.isPresent()) {
                    int txTaken = rfCapOpt.map(cap -> {
                        if (!cap.canReceive()) return 0;
                        int maxRFToGive = 100;
                        int canAcceptRF = cap.receiveEnergy(maxRFToGive, true);
                        if (canAcceptRF > 0) {
                            int txNeeded = (int) Math.ceil(canAcceptRF / 10.0);
                            int toGive = Math.min(pEnergy.getEnergy(), Math.min(txNeeded, 10));
                            int extracted = pEnergy.extractEnergyPure(toGive, false);
                            if (extracted > 0) {
                                pEnergy.addExperience(extracted * 0.15f, serverPlayer);
                                cap.receiveEnergy(extracted * 10, false);
                                return extracted;
                            }
                        }
                        return 0;
                    }).orElse(0);

                    if (txTaken > 0) {
                        changed = true;
                        if (pEnergy.getEnergy() <= 0) break;
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
        // Передаємо число 10 для локалізації
        tooltip.add(Component.translatable("tooltip.tacionian.basic_charger_item.desc", 10).withStyle(ChatFormatting.GRAY));
    }
}