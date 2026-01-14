package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
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
            if (pEnergy.getEnergy() <= 0) return;

            boolean changed = false;
            for (ItemStack target : serverPlayer.getInventory().items) {
                if (target.isEmpty() || target == stack) continue;

                var capOpt = target.getCapability(ForgeCapabilities.ENERGY);
                if (capOpt.isPresent()) {
                    final boolean[] success = {false};
                    capOpt.ifPresent(cap -> {
                        if (cap.canReceive()) {
                            int neededRF = cap.receiveEnergy(500, true);
                            if (neededRF > 0) {
                                // Виправлена математика: округлення вгору, щоб мінімум був 1 Tx
                                int txToTake = (neededRF + 9) / 10;
                                int takenTx = pEnergy.extractEnergyWithExp(txToTake, false, serverPlayer);

                                if (takenTx > 0) {
                                    cap.receiveEnergy(takenTx * 10, false);
                                    success[0] = true;
                                }
                            }
                        }
                    });
                    if (success[0]) changed = true;
                }
            }

            if (changed) {
                // Синхронізація досвіду
                pEnergy.sync(serverPlayer);

                // Візуальний ефект роботи
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