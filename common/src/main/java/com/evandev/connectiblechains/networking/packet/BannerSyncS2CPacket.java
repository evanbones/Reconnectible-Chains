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

public record BannerSyncS2CPacket(int entityId, int holderId,
                                  List<Chainable.ChainData.BannerEntry> banners) implements CustomPacketPayload {

    public static final Type<BannerSyncS2CPacket> TYPE = new Type<>(MathHelper.identifier("s2c_banner_sync"));

    private static final StreamCodec<ByteBuf, Chainable.ChainData.BannerEntry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Chainable.ChainData.BannerEntry::t,
            ByteBufCodecs.BYTE, e -> (byte) e.color().getId(),
            ByteBufCodecs.COMPOUND_TAG, Chainable.ChainData.BannerEntry::data,
            (t, colorByte, data) -> {
                DyeColor color = DyeColor.byId(colorByte & 0xFF);
                return new Chainable.ChainData.BannerEntry(t, color, data);
            }
    );

    public static final StreamCodec<ByteBuf, BannerSyncS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, BannerSyncS2CPacket::entityId,
            ByteBufCodecs.INT, BannerSyncS2CPacket::holderId,
            ByteBufCodecs.collection(ArrayList::new, ENTRY_CODEC), BannerSyncS2CPacket::banners,
            BannerSyncS2CPacket::new
    );

    public static void handle(BannerSyncS2CPacket packet, Player player) {
        if (!(player.level().getEntity(packet.entityId()) instanceof Chainable chainable)) return;

        Entity holder = player.level().getEntity(packet.holderId());
        Chainable.ChainData data = null;

        if (holder != null) {
            data = chainable.getChainData(holder);
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
            data.banners.clear();
            data.banners.addAll(packet.banners());
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
