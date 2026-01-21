package com.maxim.tacionian.blocks.charger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TachyonSafeChargerBlockEntity extends TachyonChargerBlockEntity {
    public TachyonSafeChargerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected float getEfficiency() {
        return 1.0f; // 100% ефективність (без втрат)
    }
}