package com.maxim.tacionian.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class TacionianConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue EXP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue BASE_REGEN;
    public static final ForgeConfigSpec.IntValue ENERGY_PER_LEVEL;

    static {
        BUILDER.push("Energy Core Settings");

        MAX_LEVEL = BUILDER
                .comment("Максимальний рівень тахіонного ядра. (За замовчуванням: 100)")
                .defineInRange("maxLevel", 100, 1, 1000);

        EXP_MULTIPLIER = BUILDER
                .comment("Коефіцієнт досвіду за витрачену енергію. (0.5 = 100 Tx дасть 50 XP)")
                .defineInRange("expMultiplier", 0.5, 0.01, 10.0);

        BASE_REGEN = BUILDER
                .comment("Базова регенерація енергії в секунду на 1-му рівні.")
                .defineInRange("baseRegen", 5, 0, 1000);

        ENERGY_PER_LEVEL = BUILDER
                .comment("Кількість максимальної енергії, що додається за кожен рівень.")
                .defineInRange("energyPerLevel", 500, 100, 10000);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}