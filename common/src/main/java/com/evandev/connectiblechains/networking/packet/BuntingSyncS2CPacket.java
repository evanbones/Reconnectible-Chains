package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record BuntingSyncS2CPacket(int entityId, int holderId,
                                   List<Chainable.ChainData.BuntingEntry> buntings) implements CustomPacketPayload {

    public static final Type<BuntingSyncS2CPacket> TYPE = new Type<>(MathHelper.identifier("s2c_bunting_sync"));

    private static final StreamCodec<ByteBuf, Chainable.ChainData.BuntingEntry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Chainable.ChainData.BuntingEntry::t,
            ByteBufCodecs.BYTE, e -> (byte) e.color().getId(),
            (t, id) -> new Chainable.ChainData.BuntingEntry(t, DyeColor.byId(id & 0xFF))
    );

    public static final StreamCodec<ByteBuf, BuntingSyncS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, BuntingSyncS2CPacket::entityId,
            ByteBufCodecs.INT, BuntingSyncS2CPacket::holderId,
            ByteBufCodecs.collection(ArrayList::new, ENTRY_CODEC), BuntingSyncS2CPacket::buntings,
            BuntingSyncS2CPacket::new
    );

    public static void handle(BuntingSyncS2CPacket packet, Player player) {
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
            data.buntings.clear();
            data.buntings.addAll(packet.buntings());
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
