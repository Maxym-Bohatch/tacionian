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
                energy.setPlateStabilized(true);

                // Активне скидання: якщо енергія > 90% АБО гравець присів
                boolean isHighEnergy = energy.getEnergyPercent() > 90;
                boolean isDraining = player.isCrouching();

                if (isHighEnergy || isDraining) {
                    // Визначаємо, скільки хочемо забрати (40 за тік при присіданні, або надлишок над 90%)
                    int toExtract = isDraining ? 40 : (energy.getEnergy() - (int)(energy.getMaxEnergy() * 0.90f));

                    if (toExtract > 0) {
                        // 1. Спроба передати в BlockEntity плити (а звідти в кабелі)
                        int acceptedByPlate = plateBE.receiveTacionEnergy(toExtract, false);

                        // 2. Видаляємо енергію у гравця (тільки те, що прийняла плита)
                        if (acceptedByPlate > 0) {
                            energy.extractEnergyPure(acceptedByPlate, false);
                            energy.sync(player);

                            if (level instanceof ServerLevel serverLevel) {
                                // Ефект іскор під ногами (завжди при передачі)
                                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                        player.getX(), player.getY() + 0.1, player.getZ(), 3, 0.1, 0.1, 0.1, 0.05);

                                // Якщо плита повна (не прийняла все), пускаємо вогонь у небо
                                if (acceptedByPlate < toExtract && player.tickCount % 5 == 0) {
                                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                            pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 5, 0.1, 0.5, 0.1, 0.05);
                                }
                            }

                            // Звук кожні кілька тіків
                            if (player.tickCount % 10 == 0) {
                                level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.3f, 1.2f);
                            }
                        }
                    }
                }
            });
        }
        super.stepOn(level, pos, state, entity);
    }
}