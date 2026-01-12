package com.maxim.tacionian.network;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import com.maxim.tacionian.energy.PlayerEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class EnergySyncPacket {
    private final int energy, level, experience, threshold;
    private final boolean stabilized, remoteStabilized, remoteNoDrain;

    public EnergySyncPacket(PlayerEnergy pe) {
        this.energy = pe.getEnergy();
        this.level = pe.getLevel();
        this.experience = pe.getExperience();
        this.threshold = pe.getStabilizationThreshold();
        this.stabilized = pe.isStabilized();
        this.remoteStabilized = pe.isRemoteStabilized();
        this.remoteNoDrain = pe.isRemoteNoDrain();
    }

    public EnergySyncPacket(FriendlyByteBuf buf) {
        this.energy = buf.readInt();
        this.level = buf.readInt();
        this.experience = buf.readInt();
        this.threshold = buf.readInt();
        this.stabilized = buf.readBoolean();
        this.remoteStabilized = buf.readBoolean();
        this.remoteNoDrain = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(energy);
        buf.writeInt(level);
        buf.writeInt(experience);
        buf.writeInt(threshold);
        buf.writeBoolean(stabilized);
        buf.writeBoolean(remoteStabilized);
        buf.writeBoolean(remoteNoDrain);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPlayerEnergy.receiveSync(energy, level, experience, threshold, stabilized, remoteStabilized, remoteNoDrain);
        });
        ctx.get().setPacketHandled(true);
    }
}