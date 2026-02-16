package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ChainAttachS2CPacket(int attachedEntityId, int oldHoldingEntityId, int newHoldingEntityId,
                                   int chainTypeId) implements CustomPacketPayload {

    public static final Type<ChainAttachS2CPacket> TYPE = new Type<>(MathHelper.identifier("s2c_chain_attach_packet_id"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChainAttachS2CPacket> STREAM_CODEC = CustomPacketPayload.codec(
            ChainAttachS2CPacket::write,
            ChainAttachS2CPacket::new
    );

    public ChainAttachS2CPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public ChainAttachS2CPacket(Entity attachedEntity, @Nullable Entity oldHoldingEntity, @Nullable Entity newHoldingEntity, Item souceItem) {
        this(
                attachedEntity.getId(),
                oldHoldingEntity != null ? oldHoldingEntity.getId() : 0,
                newHoldingEntity != null ? newHoldingEntity.getId() : 0,
                BuiltInRegistries.ITEM.getId(souceItem)
        );
    }

    public static void handle(ChainAttachS2CPacket packet, Player player) {
        if (player.level().getEntity(packet.attachedEntityId()) instanceof Chainable chainable) {
            chainable.addUnresolvedChainHolderId(packet.oldHoldingEntityId(), packet.newHoldingEntityId(), BuiltInRegistries.ITEM.byId(packet.chainTypeId()));
        }
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(attachedEntityId);
        buf.writeInt(oldHoldingEntityId);
        buf.writeInt(newHoldingEntityId);
        buf.writeInt(chainTypeId);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}