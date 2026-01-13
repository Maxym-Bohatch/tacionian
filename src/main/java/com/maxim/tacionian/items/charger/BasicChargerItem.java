package com.maxim.tacionian.items.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
                if (target.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                    target.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
                        int neededRF = cap.receiveEnergy(500, true);
                        if (neededRF > 0) {
                            int txToTake = neededRF / 10;
                            int takenTx = pEnergy.extractEnergyWithExp(txToTake, false, serverPlayer);
                            cap.receiveEnergy(takenTx * 10, false);
                        }
                    });
                    changed = true;
                }
            }
            if (changed && level.getGameTime() % 10 == 0) pEnergy.sync(serverPlayer);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tacionian.basic_charger_item.desc").withStyle(ChatFormatting.GRAY));
    }
}