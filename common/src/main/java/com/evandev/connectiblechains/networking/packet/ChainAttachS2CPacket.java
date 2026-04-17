package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.entity.Chainable;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ChainAttachS2CPacket(int attachedEntityId, int oldHoldingEntityId, int newHoldingEntityId,
                                   int chainTypeId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChainAttachS2CPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CommonClass.MODID, "s2c_chain_attach_packet_id"));

    public static final StreamCodec<ByteBuf, ChainAttachS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ChainAttachS2CPacket::attachedEntityId,
            ByteBufCodecs.INT, ChainAttachS2CPacket::oldHoldingEntityId,
            ByteBufCodecs.INT, ChainAttachS2CPacket::newHoldingEntityId,
            ByteBufCodecs.INT, ChainAttachS2CPacket::chainTypeId,
            ChainAttachS2CPacket::new
    );

    public ChainAttachS2CPacket(Entity attachedEntity, @Nullable Entity oldHoldingEntity, @Nullable Entity newHoldingEntity, Item sourceItem) {
        this(
                attachedEntity.getId(),
                oldHoldingEntity != null ? oldHoldingEntity.getId() : 0,
                newHoldingEntity != null ? newHoldingEntity.getId() : 0,
                BuiltInRegistries.ITEM.getId(sourceItem)
        );
    }

    public static void handle(ChainAttachS2CPacket packet, Player player) {
        if (player.level().getEntity(packet.attachedEntityId()) instanceof Chainable chainable) {
            chainable.addUnresolvedChainHolderId(packet.oldHoldingEntityId(), packet.newHoldingEntityId(), BuiltInRegistries.ITEM.byId(packet.chainTypeId()));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}