package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.networking.packet.BannerSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.networking.packet.HangingSyncS2CPacket;
import com.evandev.connectiblechains.platform.services.INetworkHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

public class FabricNetworkHelper implements INetworkHelper {

    public FabricNetworkHelper() {
    }

    @Override
    public <T> void registerClientReceiver(Class<T> type, ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHandler.register(id, decoder);
        }
    }

    @Override
    public void sendToClient(ServerPlayer player, Object packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ResourceLocation packetId = null;

        if (packet instanceof ChainAttachS2CPacket p) {
            p.write(buf);
            packetId = ChainAttachS2CPacket.TYPE;
        } else if (packet instanceof ConfigSyncPayload p) {
            p.write(buf);
            packetId = new ResourceLocation(CommonClass.MODID, "config_sync");
        } else if (packet instanceof ChainSlackSyncS2CPacket p) {
            p.write(buf);
            packetId = ChainSlackSyncS2CPacket.TYPE;
        } else if (packet instanceof BuntingSyncS2CPacket p) {
            p.write(buf);
            packetId = BuntingSyncS2CPacket.TYPE;
        } else if (packet instanceof BannerSyncS2CPacket p) {
            p.write(buf);
            packetId = BannerSyncS2CPacket.TYPE;
        } else if (packet instanceof HangingSyncS2CPacket p) {
            p.write(buf);
            packetId = HangingSyncS2CPacket.TYPE;
        }

        if (packetId != null) {
            ServerPlayNetworking.send(player, packetId, buf);
        }
    }

    @Override
    public void sendToAllClients(MinecraftServer server, Object packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ResourceLocation packetId = null;

        if (packet instanceof ChainAttachS2CPacket p) {
            p.write(buf);
            packetId = ChainAttachS2CPacket.TYPE;
        } else if (packet instanceof ConfigSyncPayload p) {
            p.write(buf);
            packetId = new ResourceLocation(CommonClass.MODID, "config_sync");
        } else if (packet instanceof ChainSlackSyncS2CPacket p) {
            p.write(buf);
            packetId = ChainSlackSyncS2CPacket.TYPE;
        } else if (packet instanceof BuntingSyncS2CPacket p) {
            p.write(buf);
            packetId = BuntingSyncS2CPacket.TYPE;
        } else if (packet instanceof BannerSyncS2CPacket p) {
            p.write(buf);
            packetId = BannerSyncS2CPacket.TYPE;
        } else if (packet instanceof HangingSyncS2CPacket p) {
            p.write(buf);
            packetId = HangingSyncS2CPacket.TYPE;
        }

        if (packetId != null && server != null) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, packetId, buf);
            }
        }
    }

    private static class ClientNetworkHandler {
        static <T> void register(ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, responseSender) -> {
                T packet = decoder.apply(buf);

                client.execute(() -> {
                    if (packet instanceof ChainAttachS2CPacket p) {
                        if (client.player != null) {
                            ChainAttachS2CPacket.handle(p, client.player);
                        }
                    } else if (packet instanceof ConfigSyncPayload p) {
                        ConfigSyncPayload.handle(p);
                    } else if (packet instanceof ChainSlackSyncS2CPacket p) {
                        if (client.player != null) {
                            ChainSlackSyncS2CPacket.handle(p, client.player);
                        }
                    } else if (packet instanceof BuntingSyncS2CPacket p) {
                        if (client.player != null) {
                            BuntingSyncS2CPacket.handle(p, client.player);
                        }
                    } else if (packet instanceof BannerSyncS2CPacket p) {
                        if (client.player != null) {
                            BannerSyncS2CPacket.handle(p, client.player);
                        }
                    } else if (packet instanceof HangingSyncS2CPacket p) {
                        if (client.player != null) {
                            HangingSyncS2CPacket.handle(p, client.player);
                        }
                    }
                });
            });
        }
    }
}
