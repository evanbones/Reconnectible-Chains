package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.networking.packet.BannerSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.networking.packet.HangingSyncS2CPacket;
import com.evandev.connectiblechains.platform.services.INetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class ForgeNetworkHelper implements INetworkHelper {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CommonClass.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private int packetId = 0;

    public ForgeNetworkHelper() {
        CHANNEL.registerMessage(packetId++, ChainBreakC2SPacket.class,
                ChainBreakC2SPacket::write,
                ChainBreakC2SPacket::new,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            ChainBreakC2SPacket.handle(msg, player);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }
        );
    }

    @Override
    public <T> void registerClientReceiver(Class<T> type, ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        CHANNEL.registerMessage(packetId++, type,
                (msg, buf) -> {
                    if (msg instanceof ChainAttachS2CPacket p) p.write(buf);
                    else if (msg instanceof ConfigSyncPayload p) p.write(buf);
                    else if (msg instanceof ChainSlackSyncS2CPacket p) p.write(buf);
                    else if (msg instanceof BuntingSyncS2CPacket p) p.write(buf);
                    else if (msg instanceof BannerSyncS2CPacket p) p.write(buf);
                    else if (msg instanceof HangingSyncS2CPacket p) p.write(buf);
                },
                decoder,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePacket(msg)));
                    ctx.get().setPacketHandled(true);
                }
        );
    }

    @Override
    public void sendToClient(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public void sendToAllClients(MinecraftServer server, Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    private static class ClientPacketHandler {
        public static void handlePacket(Object msg) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (msg instanceof ChainAttachS2CPacket p) {
                if (mc.player != null) ChainAttachS2CPacket.handle(p, mc.player);
            } else if (msg instanceof ConfigSyncPayload p) {
                ConfigSyncPayload.handle(p);
            } else if (msg instanceof ChainSlackSyncS2CPacket p) {
                if (mc.player != null) ChainSlackSyncS2CPacket.handle(p, mc.player);
            } else if (msg instanceof BuntingSyncS2CPacket p) {
                if (mc.player != null) BuntingSyncS2CPacket.handle(p, mc.player);
            } else if (msg instanceof BannerSyncS2CPacket p) {
                if (mc.player != null) BannerSyncS2CPacket.handle(p, mc.player);
            } else if (msg instanceof HangingSyncS2CPacket p) {
                if (mc.player != null) HangingSyncS2CPacket.handle(p, mc.player);
            }
        }
    }
}
