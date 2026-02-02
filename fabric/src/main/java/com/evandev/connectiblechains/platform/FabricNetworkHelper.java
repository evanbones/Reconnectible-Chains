package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

public class FabricNetworkHelper implements INetworkHelper {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(ChainAttachS2CPacket.TYPE, ChainAttachS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
    }

    @Override
    public <T> void registerClientReceiver(Class<T> type, ResourceLocation id, Function<RegistryFriendlyByteBuf, T> decoder) {
        if (type == ChainAttachS2CPacket.class) {
            ClientPlayNetworking.registerGlobalReceiver(ChainAttachS2CPacket.TYPE, (payload, context) -> {
                context.client().execute(() -> ChainAttachS2CPacket.handle(payload, context.player()));
            });
        } else if (type == ConfigSyncPayload.class) {
            ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
                context.client().execute(() -> ConfigSyncPayload.handle(payload));
            });
        }
    }

    @Override
    public void sendToClient(ServerPlayer player, Object packet) {
        if (packet instanceof CustomPacketPayload payload) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public void sendToAllClients(MinecraftServer server, Object packet) {
        if (packet instanceof CustomPacketPayload payload) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}