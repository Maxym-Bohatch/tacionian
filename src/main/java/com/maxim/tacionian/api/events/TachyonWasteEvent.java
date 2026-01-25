/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.maxim.tacionian.api.events;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
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
    public static void releaseAllEnergy(Level level, BlockPos pos, ITachyonStorage storage) {
        int amount = storage.getEnergy();
        if (amount <= 0) return;

        // Викидаємо івент
        MinecraftForge.EVENT_BUS.post(new TachyonWasteEvent(level, pos, amount));

        // Візуал
        if (!level.isClientSide) {
            ((ServerLevel)level).sendParticles(ParticleTypes.POOF,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.05);
        }
    }
}