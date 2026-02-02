package com.evandev.connectiblechains.platform.services;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

public interface INetworkHelper {
    <T> void registerClientReceiver(Class<T> type, ResourceLocation id, Function<RegistryFriendlyByteBuf, T> decoder);

    void sendToClient(ServerPlayer player, Object packet);
    void sendToAllClients(MinecraftServer server, Object packet);
}