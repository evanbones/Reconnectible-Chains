package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public record ChainSlackSyncS2CPacket(int entityId, int holderId, float slack) {
    public static final ResourceLocation TYPE = MathHelper.identifier("s2c_chain_slack_sync");

    public ChainSlackSyncS2CPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readFloat());
    }

    public static void handle(ChainSlackSyncS2CPacket packet, Player player) {
        if (player.level().getEntity(packet.entityId()) instanceof Chainable chainable) {
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
                data.customSlack = packet.slack();
            }
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(holderId);
        buf.writeFloat(slack);
    }
}