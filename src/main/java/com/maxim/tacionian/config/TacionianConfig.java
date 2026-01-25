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

package com.maxim.tacionian.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class TacionianConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue ENERGY_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue EXP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue BASE_REGEN;
    public static final ForgeConfigSpec.IntValue INTERFACE_RADIUS;
    public static final ForgeConfigSpec.IntValue NOVICE_LEVEL_THRESHOLD;

    public static final ForgeConfigSpec.IntValue HUD_X;
    public static final ForgeConfigSpec.IntValue HUD_Y;

    static {
        BUILDER.push("Energy Core Settings");
        MAX_LEVEL = BUILDER.defineInRange("maxLevel", 50, 1, 100);
        ENERGY_PER_LEVEL = BUILDER.defineInRange("energyPerLevel", 500, 100, 5000);
        EXP_MULTIPLIER = BUILDER.defineInRange("expMultiplier", 0.1, 0.01, 1.0);
        BASE_REGEN = BUILDER.defineInRange("baseRegen", 10, 0, 100);
        INTERFACE_RADIUS = BUILDER.defineInRange("interfaceRadius", 20, 1, 64);
        NOVICE_LEVEL_THRESHOLD = BUILDER.defineInRange("noviceLevelThreshold", 5, 1, 100);
        BUILDER.pop();

        BUILDER.push("HUD Visuals");
        HUD_X = BUILDER.comment("X coordinate HUD").defineInRange("hudX", 15, 0, 2000);
        HUD_Y = BUILDER.comment("Y coordinate HUD").defineInRange("hudY", 20, 0, 2000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}