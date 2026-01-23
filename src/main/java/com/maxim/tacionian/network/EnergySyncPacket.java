package com.maxim.tacionian.network;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class EnergySyncPacket {
    private final int energy, level;
    private final float experience;
    private final boolean disconnected, regenBlocked, interfaceStabilized, plateStabilized, remoteNoDrain;

    public EnergySyncPacket(PlayerEnergy storage) {
        this.energy = storage.getEnergy();
        this.level = storage.getLevel();
        this.experience = (float) storage.getExperience();
        this.disconnected = storage.isDisconnected();
        this.regenBlocked = storage.isRegenBlocked();
        this.interfaceStabilized = storage.isInterfaceStabilized();
        this.plateStabilized = storage.isPlateStabilized();
        this.remoteNoDrain = storage.isRemoteNoDrain(); // Твій предмет
    }

    public EnergySyncPacket(int e, int l, float exp, boolean d, boolean rb, boolean is, boolean ps, boolean rnd) {
        this.energy = e; this.level = l; this.experience = exp;
        this.disconnected = d; this.regenBlocked = rb;
        this.interfaceStabilized = is; this.plateStabilized = ps;
        this.remoteNoDrain = rnd;
    }

    public static void encode(EnergySyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.energy); buf.writeInt(pkt.level); buf.writeFloat(pkt.experience);
        buf.writeBoolean(pkt.disconnected); buf.writeBoolean(pkt.regenBlocked);
        buf.writeBoolean(pkt.interfaceStabilized); buf.writeBoolean(pkt.plateStabilized);
        buf.writeBoolean(pkt.remoteNoDrain);
    }

    public static EnergySyncPacket decode(FriendlyByteBuf buf) {
        return new EnergySyncPacket(buf.readInt(), buf.readInt(), buf.readFloat(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(EnergySyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPlayerEnergy.update(
                pkt.energy, pkt.level, pkt.experience,
                pkt.disconnected, pkt.regenBlocked,
                pkt.interfaceStabilized, pkt.plateStabilized, pkt.remoteNoDrain
        ));
        ctx.get().setPacketHandled(true);
    }
}