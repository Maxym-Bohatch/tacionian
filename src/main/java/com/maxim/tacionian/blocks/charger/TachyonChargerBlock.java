package com.maxim.tacionian.blocks.charger;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public class TachyonChargerBlock extends Block implements EntityBlock {
    private final boolean isSafe;

    public TachyonChargerBlock(Properties props, boolean isSafe) {
        super(props);
        this.isSafe = isSafe;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TachyonChargerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TachyonChargerBlockEntity charger) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
                int minLimit = isSafe ? (int)(pEnergy.getMaxEnergy() * 0.15f) : 0;

                if (pEnergy.getEnergy() > minLimit) {
                    // Викликаємо через Capability, щоб усе було по фен-шую Forge
                    charger.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
                        int rfAmount = 5000;
                        // Використовуємо стандартний метод receiveEnergy
                        int acceptedRF = cap.receiveEnergy(rfAmount, false);

                        if (acceptedRF > 0) {
                            // Конвертуємо Tx у RF (1:10)
                            pEnergy.extractEnergyWithExp(acceptedRF / 10, false, serverPlayer);
                            pEnergy.sync(serverPlayer);
                        }
                    });
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}