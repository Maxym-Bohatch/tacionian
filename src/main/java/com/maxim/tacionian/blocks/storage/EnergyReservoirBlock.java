package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

import javax.annotation.Nullable;
import java.util.List;

public class EnergyReservoirBlock extends Block implements EntityBlock {
    public EnergyReservoirBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyReservoirBlockEntity reservoir) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                long time = level.getGameTime();

                if (player.isShiftKeyDown()) {
                    // З БЛОКА В ГРАВЦЯ (Досвід не дається)
                    int taken = reservoir.extract(100);
                    energy.receiveEnergy(taken, false);
                } else {
                    // З ГРАВЦЯ В БЛОК (Досвід НЕ ДАЄТЬСЯ, щоб уникнути абузу)
                    // Використовуємо Pure версію
                    int given = energy.extractEnergyPure(100, false, time);
                    reservoir.fill(given);
                }

                player.displayClientMessage(Component.literal("§dРезервуар: " + reservoir.getStored() + " units"), true);
            });
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyReservoirBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tacionian.reservoir"));
    }
}