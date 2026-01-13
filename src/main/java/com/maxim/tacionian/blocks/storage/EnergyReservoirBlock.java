package com.maxim.tacionian.blocks.storage;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EnergyReservoirBlock extends Block {
    public EnergyReservoirBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(pEnergy -> {
            // Отримуємо дані про енергію в самому блоці (якщо він має BlockEntity)
            // Або якщо це простий блок, який зберігає дані в NBT (якщо це BlockEntity)

            int amount = 100; // Твоя фіксована норма за клік

            if (player.isShiftKeyDown()) {
                // ВІДДАТИ енергію гравцю (з блоку в ядро)
                // Тут має бути логіка отримання енергії з сховища блоку
                pEnergy.receiveEnergy(amount, false);
            } else {
                // ЗАБРАТИ енергію у гравця (з ядра в блок)
                // ОСЬ ТУТ БУЛА ПОМИЛКА: видалено level.getGameTime()
                pEnergy.extractEnergyPure(amount, false);
            }
        });

        return InteractionResult.SUCCESS;
    }
}