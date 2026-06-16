package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record ChainSlackSyncS2CPacket(int entityId, int holderId, float slack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChainSlackSyncS2CPacket> TYPE = new CustomPacketPayload.Type<>(MathHelper.identifier("s2c_chain_slack_sync"));

    public static final StreamCodec<ByteBuf, ChainSlackSyncS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ChainSlackSyncS2CPacket::entityId,
            ByteBufCodecs.INT, ChainSlackSyncS2CPacket::holderId,
            ByteBufCodecs.FLOAT, ChainSlackSyncS2CPacket::slack,
            ChainSlackSyncS2CPacket::new
    );

    public static void handle(ChainSlackSyncS2CPacket packet, Player player) {
        if (player.level().getEntity(packet.entityId()) instanceof Chainable chainable) {
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
                data.customSlack = packet.slack();
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}