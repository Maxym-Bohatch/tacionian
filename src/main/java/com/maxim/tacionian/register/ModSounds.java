package com.maxim.tacionian.register;

import com.maxim.tacionian.Tacionian;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "tacionian");

    // Звук енергетичного обміну (всмоктування)
    public static final RegistryObject<SoundEvent> ENERGY_CHARGE = registerSoundEvent("energy_charge");
    // Звук перемикання режимів
    public static final RegistryObject<SoundEvent> MODE_SWITCH = registerSoundEvent("mode_switch");
    // Гудіння працюючого кабелю або резервуара
    public static final RegistryObject<SoundEvent> TACHYON_HUM = registerSoundEvent("tachyon_hum");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("tacionian", name)));
    }
}