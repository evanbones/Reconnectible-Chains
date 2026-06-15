package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

public record BannerSyncS2CPacket(int entityId, int holderId, List<Chainable.ChainData.BannerEntry> banners) {
    public static final ResourceLocation TYPE = MathHelper.identifier("s2c_banner_sync");

    public BannerSyncS2CPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), readList(buf));
    }

    private static List<Chainable.ChainData.BannerEntry> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Chainable.ChainData.BannerEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            float t = buf.readFloat();
            DyeColor color = DyeColor.byId(buf.readByte() & 0xFF);
            CompoundTag data = buf.readNbt();
            list.add(new Chainable.ChainData.BannerEntry(t, color, data != null ? data : new CompoundTag()));
        }
        return list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(holderId);
        buf.writeVarInt(banners.size());
        for (Chainable.ChainData.BannerEntry e : banners) {
            buf.writeFloat(e.t());
            buf.writeByte(e.color().getId());
            buf.writeNbt(e.data());
        }
    }

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
}
