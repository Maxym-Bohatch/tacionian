/*
 *   Copyright (C) 2026 Enotien (tacionian mod)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.maxim.tacionian.network;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class EnergySyncPacket {
    private final int energy, level, maxEnergy; // Додано maxEnergy
    private final float experience;
    private final boolean disconnected, regenBlocked, interfaceStabilized, plateStabilized, remoteNoDrain;

    public EnergySyncPacket(PlayerEnergy storage) {
        this.energy = storage.getEnergy();
        this.level = storage.getLevel();
        this.maxEnergy = storage.getMaxEnergy(); // Беремо актуальне значення з сервера
        this.experience = (float) storage.getExperience();
        this.disconnected = storage.isDisconnected();
        this.regenBlocked = storage.isRegenBlocked();
        this.interfaceStabilized = storage.isInterfaceStabilized();
        this.plateStabilized = storage.isPlateStabilized();
        this.remoteNoDrain = storage.isRemoteNoDrain();
    }

    public EnergySyncPacket(int e, int l, int me, float exp, boolean d, boolean rb, boolean is, boolean ps, boolean rnd) {
        this.energy = e; this.level = l; this.maxEnergy = me; this.experience = exp;
        this.disconnected = d; this.regenBlocked = rb;
        this.interfaceStabilized = is; this.plateStabilized = ps;
        this.remoteNoDrain = rnd;
    }

    public static void encode(EnergySyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.energy);
        buf.writeInt(pkt.level);
        buf.writeInt(pkt.maxEnergy); // Записуємо
        buf.writeFloat(pkt.experience);
        buf.writeBoolean(pkt.disconnected);
        buf.writeBoolean(pkt.regenBlocked);
        buf.writeBoolean(pkt.interfaceStabilized);
        buf.writeBoolean(pkt.plateStabilized);
        buf.writeBoolean(pkt.remoteNoDrain);
    }

    public static EnergySyncPacket decode(FriendlyByteBuf buf) {
        return new EnergySyncPacket(
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat(), // Читаємо int для maxEnergy
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(EnergySyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPlayerEnergy.update(
                pkt.energy, pkt.level, pkt.maxEnergy, pkt.experience, // Передаємо pkt.maxEnergy
                pkt.disconnected, pkt.regenBlocked,
                pkt.interfaceStabilized, pkt.plateStabilized, pkt.remoteNoDrain
        ));
        ctx.get().setPacketHandled(true);
    }
}