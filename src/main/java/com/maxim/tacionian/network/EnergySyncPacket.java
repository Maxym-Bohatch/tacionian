package com.maxim.tacionian.network;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnergySyncPacket {
    private final int energy, level, experience, threshold;
    private final boolean stabilized, remoteStabilized, remoteNoDrain;

    // Конструктор для створення пакета з даних Capability на сервері
    public EnergySyncPacket(PlayerEnergy storage) {
        this.energy = storage.getEnergy();
        this.level = storage.getLevel();
        this.experience = storage.getExperience();
        this.threshold = 10;
        this.stabilized = storage.isStabilized();
        this.remoteStabilized = storage.isRemoteStabilized();
        this.remoteNoDrain = storage.isRemoteNoDrain();
    }

    // Допоміжний конструктор для декодування
    public EnergySyncPacket(int energy, int level, int experience, int threshold,
                            boolean stabilized, boolean remoteStabilized, boolean remoteNoDrain) {
        this.energy = energy;
        this.level = level;
        this.experience = experience;
        this.threshold = threshold;
        this.stabilized = stabilized;
        this.remoteStabilized = remoteStabilized;
        this.remoteNoDrain = remoteNoDrain;
    }

    public static void encode(EnergySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.energy);
        buf.writeInt(msg.level);
        buf.writeInt(msg.experience);
        buf.writeInt(msg.threshold);
        buf.writeBoolean(msg.stabilized);
        buf.writeBoolean(msg.remoteStabilized);
        buf.writeBoolean(msg.remoteNoDrain);
    }

    public static EnergySyncPacket decode(FriendlyByteBuf buf) {
        return new EnergySyncPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(EnergySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Викликаємо твій метод receiveSync з усіма аргументами
            ClientPlayerEnergy.receiveSync(msg.energy, msg.level, msg.experience,
                    msg.threshold, msg.stabilized,
                    msg.remoteStabilized, msg.remoteNoDrain);
        });
        ctx.get().setPacketHandled(true);
    }
}