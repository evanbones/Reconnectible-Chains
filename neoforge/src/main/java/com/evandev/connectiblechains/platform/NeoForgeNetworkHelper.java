package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.platform.services.INetworkHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Function;

public class NeoForgeNetworkHelper implements INetworkHelper {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                ChainAttachS2CPacket.TYPE,
                ChainAttachS2CPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ChainAttachS2CPacket.handle(payload, context.player()))
        );

        registrar.playToClient(
                ConfigSyncPayload.TYPE,
                ConfigSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ConfigSyncPayload.handle(payload))
        );

        registrar.playToClient(
                ChainSlackSyncS2CPacket.TYPE,
                ChainSlackSyncS2CPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ChainSlackSyncS2CPacket.handle(payload, context.player()))
        );

        registrar.playToServer(
                ChainBreakC2SPacket.TYPE,
                ChainBreakC2SPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ChainBreakC2SPacket.handle(payload, context.player()))
        );
    }

    @Override
    public <T> void registerClientReceiver(Class<T> type, ResourceLocation id, Function<RegistryFriendlyByteBuf, T> decoder) {
    }

    @Override
    public void sendToClient(ServerPlayer player, Object packet) {
        if (packet instanceof CustomPacketPayload payload) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendToAllClients(MinecraftServer server, Object packet) {
        if (packet instanceof CustomPacketPayload payload) {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }
}