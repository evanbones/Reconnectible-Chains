package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record HangingSyncS2CPacket(int entityId, int holderId,
                                   List<Chainable.ChainData.HangingEntry> hangings) implements CustomPacketPayload {

    public static final Type<HangingSyncS2CPacket> TYPE = new Type<>(MathHelper.identifier("s2c_hanging_sync"));

    private static final StreamCodec<ByteBuf, Chainable.ChainData.HangingEntry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Chainable.ChainData.HangingEntry::t,
            ResourceLocation.STREAM_CODEC, Chainable.ChainData.HangingEntry::blockId,
            Chainable.ChainData.HangingEntry::new
    );

    public static final StreamCodec<ByteBuf, HangingSyncS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HangingSyncS2CPacket::entityId,
            ByteBufCodecs.INT, HangingSyncS2CPacket::holderId,
            ByteBufCodecs.collection(ArrayList::new, ENTRY_CODEC), HangingSyncS2CPacket::hangings,
            HangingSyncS2CPacket::new
    );

    public static void handle(HangingSyncS2CPacket packet, Player player) {
        if (!(player.level().getEntity(packet.entityId()) instanceof Chainable chainable)) return;

        Entity holder = player.level().getEntity(packet.holderId());
        Chainable.ChainData data = null;

        if (holder != null) {
            data = chainable.getChainData(holder);
            if (data == null) data = chainable.getChainData(holder);
        }

        if (data == null) {
            for (Chainable.ChainData chainData : chainable.getChainDataSet()) {
                if (chainData.unresolvedChainHolderId == packet.holderId()) {
                    data = chainData;
                    break;
                }
            }
        }

        if (data != null) {
            data.hangings.clear();
            data.hangings.addAll(packet.hangings());
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
