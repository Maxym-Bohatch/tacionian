package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlock extends BaseEntityBlock {
    private final boolean isSafe;

    public TachyonChargerBlock(Properties props, boolean isSafe) {
        super(props);
        this.isSafe = isSafe;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TachyonChargerBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CHARGER_BE.get(), TachyonChargerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TachyonChargerBlockEntity charger) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int minLimit = isSafe ? (int)(pEnergy.getMaxEnergy() * 0.15f) : 0;

                if (pEnergy.getEnergy() > minLimit) {
                    int acceptedTx = charger.receiveTacionEnergy(50, false);
                    if (acceptedTx > 0) {
                        pEnergy.extractEnergyWithExp(acceptedTx, false, serverPlayer);
                        pEnergy.sync(serverPlayer);
                        // ВЛАСНИЙ ЗВУК: Процес зарядки
                        level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.7f, 1.4f);
                    }
                } else {
                    // Використовуємо системний звук помилки, бо це негативна подія
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 1.0f, 0.5f);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}