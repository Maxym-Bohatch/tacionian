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