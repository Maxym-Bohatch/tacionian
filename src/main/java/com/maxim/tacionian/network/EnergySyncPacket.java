/*
 * Copyright (C) 2026 Enotien (tacionian mod)
 * License: GPLv3
 */

package com.maxim.tacionian.network;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class EnergySyncPacket {
    private final int energy, level, maxEnergy, stabilizedTimer;
    private final float experience;
    private final boolean disconnected, regenBlocked, interfaceStabilized, plateStabilized, remoteNoDrain, remoteBlocked, pushbackEnabled, globalStabilized;

    public EnergySyncPacket(PlayerEnergy storage) {
        this.energy = storage.getEnergy();
        this.level = storage.getLevel();
        this.maxEnergy = storage.getMaxEnergy();
        this.stabilizedTimer = storage.getStabilizedTimer();
        this.experience = (float) storage.getExperience();
        this.disconnected = storage.isDisconnected();
        this.regenBlocked = storage.isRegenBlocked();
        this.interfaceStabilized = storage.isInterfaceStabilized();
        this.plateStabilized = storage.isPlateStabilized();
        this.remoteNoDrain = storage.isRemoteNoDrain();
        this.remoteBlocked = storage.isRemoteAccessBlocked(); // Нове
        this.pushbackEnabled = storage.isPushbackEnabled(); // Нове
        this.globalStabilized = storage.isStabilizedLogicActive();
    }

    // Декодер конструктор
    public EnergySyncPacket(int e, int l, int me, int st, float exp, boolean d, boolean rb, boolean is, boolean ps, boolean rnd, boolean rbk, boolean pe, boolean gs) {
        this.energy = e; this.level = l; this.maxEnergy = me; this.stabilizedTimer = st;
        this.experience = exp; this.disconnected = d; this.regenBlocked = rb;
        this.interfaceStabilized = is; this.plateStabilized = ps; this.remoteNoDrain = rnd;
        this.remoteBlocked = rbk; this.pushbackEnabled = pe; this.globalStabilized = gs;
    }

    public static void encode(EnergySyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.energy);
        buf.writeInt(pkt.level);
        buf.writeInt(pkt.maxEnergy);
        buf.writeInt(pkt.stabilizedTimer);
        buf.writeFloat(pkt.experience);
        buf.writeBoolean(pkt.disconnected);
        buf.writeBoolean(pkt.regenBlocked);
        buf.writeBoolean(pkt.interfaceStabilized);
        buf.writeBoolean(pkt.plateStabilized);
        buf.writeBoolean(pkt.remoteNoDrain);
        buf.writeBoolean(pkt.remoteBlocked);
        buf.writeBoolean(pkt.pushbackEnabled);
        buf.writeBoolean(pkt.globalStabilized);
    }

    public static EnergySyncPacket decode(FriendlyByteBuf buf) {
        return new EnergySyncPacket(
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readFloat(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean()
        );
    }

    public static void handle(EnergySyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPlayerEnergy.update(
                pkt.energy, pkt.level, pkt.maxEnergy, pkt.stabilizedTimer, pkt.experience,
                pkt.disconnected, pkt.regenBlocked, pkt.interfaceStabilized,
                pkt.plateStabilized, pkt.remoteNoDrain, pkt.remoteBlocked, pkt.pushbackEnabled, pkt.globalStabilized
        ));
        ctx.get().setPacketHandled(true);
    }
}