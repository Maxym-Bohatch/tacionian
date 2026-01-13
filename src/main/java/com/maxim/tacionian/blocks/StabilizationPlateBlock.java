package com.maxim.tacionian.blocks;

import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.core.BlockPos;
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
                // Встановлюємо статус стабілізації
                energy.setStabilized(true);

                // Якщо енергія вище 95%, плита "спалює" надлишок без досвіду
                if (energy.getEnergyPercent() > 95) {
                    energy.extractEnergyPure(20, false);
                }
            });
        }
        super.stepOn(level, pos, state, entity);
    }
}