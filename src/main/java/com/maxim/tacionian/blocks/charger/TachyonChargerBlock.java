package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlock extends BaseEntityBlock {
    private final boolean isSafe;

    public TachyonChargerBlock(Properties props, boolean isSafe) {
        super(props);
        this.isSafe = isSafe;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TachyonChargerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CHARGER_BE.get(),
                TachyonChargerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TachyonChargerBlockEntity charger) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                // Мінімальний ліміт енергії гравця (безпечний режим)
                int minLimit = isSafe ? (int)(pEnergy.getMaxEnergy() * 0.15f) : 0;

                if (pEnergy.getEnergy() > minLimit) {
                    // Заряджаємо внутрішній буфер блоку (макс за раз 50 Tx = 500 RF)
                    int acceptedTx = charger.receiveTacionEnergy(50, false);

                    if (acceptedTx > 0) {
                        pEnergy.extractEnergyWithExp(acceptedTx, false, serverPlayer);
                        pEnergy.sync(serverPlayer);

                        player.displayClientMessage(Component.literal("§b⚡ Зарядка: §f" +
                                charger.getEnergy() + "/" + charger.getMaxCapacity() + " Tx"), true);
                    } else {
                        player.displayClientMessage(Component.literal("§cБлок повністю заряджений!").withStyle(ChatFormatting.RED), true);
                    }
                } else {
                    player.displayClientMessage(Component.literal("§cЗанадто низький рівень енергії!").withStyle(ChatFormatting.RED), true);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}