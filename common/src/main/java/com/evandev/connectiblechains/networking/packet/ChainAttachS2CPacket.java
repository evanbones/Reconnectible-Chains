package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public record ChainAttachS2CPacket(int attachedEntityId, int oldHoldingEntityId, int newHoldingEntityId, int chainTypeId) {
    public static final ResourceLocation TYPE = MathHelper.identifier("s2c_chain_attach_packet_id");

    public ChainAttachS2CPacket(Entity attachedEntity, @Nullable Entity oldHoldingEntity, @Nullable Entity newHoldingEntity, Item souceItem) {
        this(attachedEntity.getId(), oldHoldingEntity != null ? oldHoldingEntity.getId() : 0, newHoldingEntity != null ? newHoldingEntity.getId() : 0, BuiltInRegistries.ITEM.getId(souceItem));
    }

    public ChainAttachS2CPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(attachedEntityId);
        buf.writeInt(oldHoldingEntityId);
        buf.writeInt(newHoldingEntityId);
        buf.writeInt(chainTypeId);
    }

    public static void handle(ChainAttachS2CPacket packet, Player player) {
        if (player.level().getEntity(packet.attachedEntityId()) instanceof Chainable chainable) {
            chainable.addUnresolvedChainHolderId(packet.oldHoldingEntityId(), packet.newHoldingEntityId(), BuiltInRegistries.ITEM.byId(packet.chainTypeId()));
        }
    }
}