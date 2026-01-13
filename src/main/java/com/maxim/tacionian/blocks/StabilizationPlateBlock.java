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
                // Активуємо віддалену стабілізацію
                energy.setRemoteStabilized(true);

                // Плита автоматично скидає енергію до 95%, якщо є перевантаження
                if (energy.getEnergyPercent() > 95) {
                    // Використовуємо пряме встановлення енергії для стабілізації
                    energy.setEnergy((int)(energy.getMaxEnergy() * 0.95f));
                }
            });
        }
        super.stepOn(level, pos, state, entity);
    }
}