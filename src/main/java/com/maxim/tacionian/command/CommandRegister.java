package com.maxim.tacionian.command;

import net.minecraftforge.event.RegisterCommandsEvent;

public class CommandRegister {
    public static void onRegister(RegisterCommandsEvent event) {
        EnergyCommand.register(event.getDispatcher());
    }
}
