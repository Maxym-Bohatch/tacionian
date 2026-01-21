package com.maxim.tacionian.blocks;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModBlockEntities;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StabilizationPlateBlock extends BaseEntityBlock {
    public StabilizationPlateBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StabilizationPlateBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STABILIZER_PLATE_BE.get(), StabilizationPlateBlockEntity::tick);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof StabilizationPlateBlockEntity plateBE)) return;

            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                // Миттєвий захист від дебафів
                energy.setRemoteStabilized(true);

                if (energy.getEnergyPercent() > 95) {
                    int energyBefore = energy.getEnergy();
                    int stableLimit = (int)(energy.getMaxEnergy() * 0.95f);
                    int releasedEnergy = energyBefore - stableLimit;

                    // 1. Спроба злити в кабелі через BlockEntity плити
                    int acceptedByCables = plateBE.receiveTacionEnergy(releasedEnergy, false);

                    // 2. Те, що не влізло, стає "викидом" у небо
                    int leftovers = releasedEnergy - acceptedByCables;

                    // Оновлюємо енергію гравця
                    energy.setEnergy(stableLimit);
                    energy.sync(player);

                    if (level instanceof ServerLevel serverLevel) {
                        // Ефект заземлення (іскри біля ніг)
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.1);

                        // Ефект викиду в небо (тільки якщо кабелі повні)
                        if (leftovers > 0) {
                            for (int i = 1; i < 6; i++) {
                                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                        pos.getX() + 0.5, pos.getY() + (i * 0.8), pos.getZ() + 0.5,
                                        2, 0.1, 0.2, 0.1, 0.02);
                            }
                        }
                    }

                    // Звук стабілізації
                    level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.5f, 0.7f);
                }
            });
        }
        super.stepOn(level, pos, state, entity);
    }
}