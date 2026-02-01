package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.platform.services.INetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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

    @Override
    public <T> void registerClientReceiver(Class<T> type, ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        CHANNEL.registerMessage(packetId++, type,
                (msg, buf) -> {
                    if (msg instanceof ChainAttachS2CPacket p) p.write(buf);
                    if (msg instanceof ConfigSyncPayload p) p.write(buf);
                },
                decoder,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        if (msg instanceof ChainAttachS2CPacket p) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null) {
                                ChainAttachS2CPacket.handle(p, mc.player);
                            }
                        }
                        if (msg instanceof ConfigSyncPayload p) {
                            ConfigSyncPayload.handle(p);
                        }
                    });
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
}