package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.api.events.TachyonWasteEvent;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyReservoirBlock extends Block implements EntityBlock {
    public EnergyReservoirBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyReservoirBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Логіка працює тільки на сервері (щоб дані були точними)
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyReservoirBlockEntity reservoir) {

            // 1. Якщо рука порожня і Shift не натиснуто -> ТІЛЬКИ ІНФОРМАЦІЯ
            if (player.getItemInHand(hand).isEmpty() && !player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy",
                        reservoir.getEnergy(), reservoir.getMaxCapacity()).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS; // Виходимо, енергію не чіпаємо
            }

            // 2. Логіка передачі енергії (якщо в руці щось є або натиснуто Shift)
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int amount = 500;
                boolean actionHappened = false;

                if (player.isShiftKeyDown()) {
                    // Забираємо з блоку -> гравцеві
                    int extractedFromBlock = reservoir.extractTacionEnergy(amount, false);
                    if (extractedFromBlock > 0) {
                        pEnergy.receiveEnergy(extractedFromBlock, false);
                        player.displayClientMessage(Component.literal("§a[<] Витягнуто: " + extractedFromBlock + " Tx"), true);
                        actionHappened = true;
                    }
                } else {
                    // Забираємо у гравця -> в блок (Тільки якщо в руці предмет, наприклад, "провідник")
                    int takenFromPlayer = pEnergy.extractEnergyPure(amount, false);
                    if (takenFromPlayer > 0) {
                        reservoir.receiveTacionEnergy(takenFromPlayer, false);
                        player.displayClientMessage(Component.literal("§c[>] Залито: " + takenFromPlayer + " Tx"), true);
                        actionHappened = true;
                    }
                }

                if (actionHappened) {
                    pEnergy.sync((ServerPlayer) player);
                } else {
                    // Якщо нічого не сталося (блок повний або гравець пустий) - показуємо статус
                    player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy",
                            reservoir.getEnergy(), reservoir.getMaxCapacity()).withStyle(ChatFormatting.RED), true);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    // ... (Методи onRemove та appendHoverText залишаються без змін) ...
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnergyReservoirBlockEntity reservoir) {
                int leftover = reservoir.getEnergy();
                if (leftover > 0 && !level.isClientSide) {
                    MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, leftover));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = stack.getTagElement("BlockEntityTag");
        int energy = (nbt != null) ? nbt.getInt("StoredTacion") : 0;

        tooltip.add(Component.translatable("tooltip.tacionian.energy_reservoir.energy", energy, 25000).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tacionian.energy_reservoir.desc").withStyle(ChatFormatting.GRAY));
    }
}