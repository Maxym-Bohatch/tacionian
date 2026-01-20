package com.maxim.tacionian.items.energy;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyCellItem extends Item {
    private static final int MAX_ENERGY = 3000;

    public EnergyCellItem(Properties props) {
        super(props.stacksTo(1));
    }

    // 1. ВЗАЄМОДІЯ З БЛОКАМИ (Клік по резервуару)
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (be != null) {
            return be.getCapability(ModCapabilities.TACHYON_STORAGE, context.getClickedFace()).map(storage -> {
                CompoundTag nbt = stack.getOrCreateTag();
                int currentEnergy = nbt.getInt("energy");

                if (player != null && player.isShiftKeyDown()) {
                    // РОЗРЯДКА ПРЕДМЕТА В БЛОК
                    int toGive = storage.receiveTacionEnergy(currentEnergy, false);
                    nbt.putInt("energy", currentEnergy - toGive);
                    player.displayClientMessage(Component.translatable("message.tacionian.discharged", toGive).withStyle(ChatFormatting.RED), true);
                } else {
                    // ЗАРЯДКА ПРЕДМЕТА З БЛОКА
                    int space = MAX_ENERGY - currentEnergy;
                    int toTake = storage.extractTacionEnergy(space, false);
                    nbt.putInt("energy", currentEnergy + toTake);
                    if (player != null) {
                        player.displayClientMessage(Component.translatable("message.tacionian.charged", toTake).withStyle(ChatFormatting.GREEN), true);
                    }
                }
                return InteractionResult.CONSUME;
            }).orElse(InteractionResult.PASS);
        }
        return InteractionResult.PASS;
    }

    // 2. ВЗАЄМОДІЯ З ГРАВЦЕМ (Твій старий код для переливання в себе)
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);

        serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            CompoundTag nbt = stack.getOrCreateTag();
            int stored = nbt.getInt("energy");
            int step = 100; // Крок передачі

            if (serverPlayer.isShiftKeyDown()) {
                int toGive = Math.min(Math.min(stored, step), pEnergy.getMaxEnergy() - pEnergy.getEnergy());
                if (toGive > 0) {
                    pEnergy.receiveEnergy(toGive, false);
                    nbt.putInt("energy", stored - toGive);
                }
            } else {
                int spaceInCell = MAX_ENERGY - stored;
                int toTake = Math.min(Math.min(pEnergy.getEnergy(), step), spaceInCell);
                if (toTake > 0) {
                    pEnergy.extractEnergyPure(toTake, false);
                    nbt.putInt("energy", stored + toTake);
                }
            }
            pEnergy.sync(serverPlayer);
        });
        return InteractionResultHolder.success(stack);
    }

    // 3. ВІЗУАЛЬНА СМУЖКА ЗАРЯДУ
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true; // Смужка видна завжди
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = stack.hasTag() ? stack.getTag().getInt("energy") : 0;
        return Math.round((float) energy * 13.0F / (float) MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Колір змінюється від червоного до бірюзового (тахіонного)
        return Mth.hsvToRgb(0.55F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int energy = stack.hasTag() ? stack.getTag().getInt("energy") : 0;
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.charge", energy, MAX_ENERGY).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.tacionian.energy_cell.block_hint").withStyle(ChatFormatting.GRAY));
    }
}