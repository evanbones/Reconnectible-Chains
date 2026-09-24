package com.evandev.connectiblechains.platform.fabric;

//? if fabric {

import com.evandev.connectiblechains.networking.packet.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricClientNetworkHelper {

    public static <T> void registerClientReceiver(Class<T> type) {
        if (type == ChainAttachS2CPacket.class) {
            ClientPlayNetworking.registerGlobalReceiver(ChainAttachS2CPacket.TYPE, (payload, context) -> {
                context.client().execute(() -> ChainAttachS2CPacket.handle(payload, context.player()));
            });
        }

        if (type == ConfigSyncPayload.class) {
            ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
                context.client().execute(() -> ConfigSyncPayload.handle(payload));
            });
        }

        if (type == ChainSlackSyncS2CPacket.class) {
            ClientPlayNetworking.registerGlobalReceiver(ChainSlackSyncS2CPacket.TYPE, (payload, context) -> {
                context.client().execute(() -> ChainSlackSyncS2CPacket.handle(payload, context.player()));
            });
        }

        if (type == BuntingSyncS2CPacket.class) {
            ClientPlayNetworking.registerGlobalReceiver(BuntingSyncS2CPacket.TYPE, (payload, context) -> {
                context.client().execute(() -> BuntingSyncS2CPacket.handle(payload, context.player()));
            });
        }

        if (type == BannerSyncS2CPacket.class) {
            ClientPlayNetworking.registerGlobalReceiver(BannerSyncS2CPacket.TYPE, (payload, context) -> {
                context.client().execute(() -> BannerSyncS2CPacket.handle(payload, context.player()));
            });
        }

        if (type == HangingSyncS2CPacket.class) {
            ClientPlayNetworking.registerGlobalReceiver(HangingSyncS2CPacket.TYPE, (payload, context) -> {
                context.client().execute(() -> HangingSyncS2CPacket.handle(payload, context.player()));
            });
        }
    }
}
//?}
