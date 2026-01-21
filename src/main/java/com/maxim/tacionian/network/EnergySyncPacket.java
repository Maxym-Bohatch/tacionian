package com.maxim.tacionian.network;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnergySyncPacket {
    private final int energy, level, experience, maxEnergy, requiredExp;
    private final boolean stabilized, remoteStabilized, remoteNoDrain, jammed;

    // Конструктор для сервера: витягуємо дані з капабіліті
    public EnergySyncPacket(PlayerEnergy storage) {
        this.energy = storage.getEnergy();
        this.level = storage.getLevel();
        this.experience = storage.getExperience();
        this.maxEnergy = storage.getMaxEnergy();
        this.requiredExp = storage.getRequiredExp();
        this.stabilized = storage.isStabilized();
        this.remoteStabilized = storage.isRemoteStabilized();
        this.remoteNoDrain = storage.isRemoteNoDrain();
        this.jammed = storage.isConnectionBlocked(); // Передаємо стан глушилки
    }

    // Конструктор для декодування
    public EnergySyncPacket(int energy, int level, int experience, int maxEnergy, int requiredExp,
                            boolean stabilized, boolean remoteStabilized, boolean remoteNoDrain, boolean jammed) {
        this.energy = energy;
        this.level = level;
        this.experience = experience;
        this.maxEnergy = maxEnergy;
        this.requiredExp = requiredExp;
        this.stabilized = stabilized;
        this.remoteStabilized = remoteStabilized;
        this.remoteNoDrain = remoteNoDrain;
        this.jammed = jammed;
    }

    public static void encode(EnergySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.energy);
        buf.writeInt(msg.level);
        buf.writeInt(msg.experience);
        buf.writeInt(msg.maxEnergy);
        buf.writeInt(msg.requiredExp);
        buf.writeBoolean(msg.stabilized);
        buf.writeBoolean(msg.remoteStabilized);
        buf.writeBoolean(msg.remoteNoDrain);
        buf.writeBoolean(msg.jammed);
    }

    public static EnergySyncPacket decode(FriendlyByteBuf buf) {
        return new EnergySyncPacket(
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()
        );
    }

    public static void handle(EnergySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Оновлюємо клієнтське сховище
            ClientPlayerEnergy.receiveSync(
                    msg.energy,
                    msg.level,
                    msg.experience,
                    msg.maxEnergy,
                    msg.requiredExp,
                    msg.stabilized,
                    msg.remoteStabilized,
                    msg.remoteNoDrain,
                    msg.jammed
            );
        });
        ctx.get().setPacketHandled(true);
    }
}