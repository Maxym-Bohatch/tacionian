package com.maxim.tacionian.blocks;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import com.maxim.tacionian.register.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class StabilizationPlateBlock extends Block {
    public StabilizationPlateBlock(Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energy -> {
                energy.setRemoteStabilized(true);

                if (energy.getEnergyPercent() > 95) {
                    energy.setEnergy((int)(energy.getMaxEnergy() * 0.95f));

                    // ВЛАСНИЙ ЗВУК: Стабілізація (низький тон)
                    level.playSound(null, pos, ModSounds.ENERGY_CHARGE.get(), SoundSource.BLOCKS, 0.5f, 0.7f);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.1);
                    }
                }
            });
        }
        super.stepOn(level, pos, state, entity);
    }
}