package com.maxim.tacionian.register;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public class ModDamageSources {
    // Ресурсний ключ для нашої шкоди (має збігатися з ключем у локалізації без "death.attack.")
    public static final ResourceKey<DamageType> TACHYON_ENERGY =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("tacionian", "energy"));

    public static DamageSource getTachyonDamage(Level level) {
        // Отримуємо тип шкоди з реєстру світу
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TACHYON_ENERGY);

        return new DamageSource(holder);
    }
}