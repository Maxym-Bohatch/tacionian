package com.maxim.tacionian.register;

import com.maxim.tacionian.api.energy.ITachyonStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ModCapabilities {
    // Це посилання, яке використовуватимуть кабелі та машини
    public static final Capability<ITachyonStorage> TACHYON_STORAGE = CapabilityManager.get(new CapabilityToken<>() {});
}