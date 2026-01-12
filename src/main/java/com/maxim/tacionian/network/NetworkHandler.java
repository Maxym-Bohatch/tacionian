package com.maxim.tacionian.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private static int index = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("tacionian", "main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        CHANNEL.messageBuilder(EnergySyncPacket.class, index++)
                .encoder(EnergySyncPacket::encode)
                .decoder(EnergySyncPacket::new)
                .consumerMainThread(EnergySyncPacket::handle)
                .add();
    }
}
