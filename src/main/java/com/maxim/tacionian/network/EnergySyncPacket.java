package com.maxim.tacionian.network;

import com.maxim.tacionian.energy.PlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergyProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnergySyncPacket {
    private final int energy;
    private final int level;
    private final int experience;

    public EnergySyncPacket(int energy, int level, int experience) {
        this.energy = energy;
        this.level = level;
        this.experience = experience;
    }

    public EnergySyncPacket(PlayerEnergy storage) {
        this.energy = storage.getEnergy();
        this.level = storage.getLevel();
        this.experience = storage.getExperience();
    }

    public static void encode(EnergySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.energy);
        buf.writeInt(msg.level);
        buf.writeInt(msg.experience);
    }

    public static EnergySyncPacket decode(FriendlyByteBuf buf) {
        return new EnergySyncPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(EnergySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Клієнтська сторона
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(cap -> {
                    cap.setEnergy(msg.energy);
                    // Тут можна додати методи setLevel/setExp в PlayerEnergy, якщо треба відображати їх
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}