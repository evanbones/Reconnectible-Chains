package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricClientNetworkHelper {

    public static <T> void registerClientReceiver(Class<T> type) {
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
}