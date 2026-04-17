package com.evandev.connectiblechains;

import com.evandev.connectiblechains.command.ConnectChainCommand;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.platform.FabricNetworkHelper;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.InteractionResult;

public class ConnectibleChainsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        FabricNetworkHelper.init();
        CommonClass.init();

        PayloadTypeRegistry.serverboundPlay().register(ChainBreakC2SPacket.TYPE, ChainBreakC2SPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ChainBreakC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ChainBreakC2SPacket.handle(payload, context.player());
            });
        });

        UseBlockCallback.EVENT.register(ChainItemCallbacks::chainUseEvent);

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (ChainRaycastHelper.tryAdjustSlack(player, hand)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CommonClass.fileConfig.syncToClient(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ConnectChainCommand.register(dispatcher);
        });
    }
}