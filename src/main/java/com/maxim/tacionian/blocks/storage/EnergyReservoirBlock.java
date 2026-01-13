package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyReservoirBlockEntity reservoir) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int amount = 500; // Кількість енергії за один клік

                if (player.isShiftKeyDown()) {
                    // ЗАБРАТИ з блоку -> ВІДДАТИ гравцю
                    int extractedFromBlock = reservoir.extractTacionEnergy(amount, false);
                    if (extractedFromBlock > 0) {
                        pEnergy.receiveEnergy(extractedFromBlock, false);
                    }
                } else {
                    // ЗАБРАТИ у гравця -> ВІДДАТИ в блок
                    // Використовуємо extractEnergyPure, бо це просто зберігання, а не "спалювання" для досвіду
                    int takenFromPlayer = pEnergy.extractEnergyPure(amount, false);
                    if (takenFromPlayer > 0) {
                        reservoir.receiveTacionEnergy(takenFromPlayer, false);
                    }
                }

                // Виводимо стан резервуара в Actionbar (над інвентарем)
                player.displayClientMessage(Component.translatable("tooltip.tacionian.energy_reservoir.energy",
                        reservoir.getEnergy(), reservoir.getMaxCapacity()).withStyle(ChatFormatting.AQUA), true);

                pEnergy.sync((ServerPlayer) player);
            });
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.tacionian.energy_reservoir.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.tacionian.energy_reservoir.controls").withStyle(ChatFormatting.YELLOW));
    }
}
