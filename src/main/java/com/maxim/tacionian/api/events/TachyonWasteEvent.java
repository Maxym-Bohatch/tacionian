package com.maxim.tacionian.api.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

public class TachyonWasteEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final int amount;

    public TachyonWasteEvent(Level level, BlockPos pos, int amount) {
        this.level = level;
        this.pos = pos;
        this.amount = amount;
    }

    public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public int getAmount() { return amount; }
}