package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public record HangingSyncS2CPacket(int entityId, int holderId, List<Chainable.ChainData.HangingEntry> hangings) {
    public static final ResourceLocation TYPE = MathHelper.identifier("s2c_hanging_sync");

    public HangingSyncS2CPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), readList(buf));
    }

    private static List<Chainable.ChainData.HangingEntry> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Chainable.ChainData.HangingEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            float t = buf.readFloat();
            ResourceLocation blockId = new ResourceLocation(buf.readUtf());
            list.add(new Chainable.ChainData.HangingEntry(t, blockId));
        }
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(holderId);
        buf.writeVarInt(hangings.size());
        for (Chainable.ChainData.HangingEntry e : hangings) {
            buf.writeFloat(e.t());
            buf.writeUtf(e.blockId().toString());
        }
    }

    public static void handle(HangingSyncS2CPacket packet, Player player) {
        if (!(player.level().getEntity(packet.entityId()) instanceof Chainable chainable)) return;

        Chainable.ChainData data = null;

        for (Chainable.ChainData chainData : chainable.getChainDataSet()) {
            if (chainData.unresolvedChainHolderId == packet.holderId()) {
                data = chainData;
                break;
            }
        }

        if (data == null) {
            Entity holder = player.level().getEntity(packet.holderId());
            if (holder != null) {
                data = chainable.getChainData(holder);
            }
        }

        if (data != null) {
            data.hangings.clear();
            data.hangings.addAll(packet.hangings());
        }
    }
}
