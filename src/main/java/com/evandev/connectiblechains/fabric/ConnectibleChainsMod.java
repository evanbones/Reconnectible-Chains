package com.evandev.connectiblechains.fabric;

//? if fabric {

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.command.ConnectChainCommand;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.networking.packet.DecorationRemoveC2SPacket;
import com.evandev.connectiblechains.platform.fabric.FabricNetworkHelper;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.InteractionResult;
//? if <26.1
//import net.minecraft.world.InteractionResultHolder;

public class ConnectibleChainsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        FabricNetworkHelper.init();
        CommonClass.init();

        //? if >=26.1 {
        PayloadTypeRegistry.serverboundPlay().register(ChainBreakC2SPacket.TYPE, ChainBreakC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DecorationRemoveC2SPacket.TYPE, DecorationRemoveC2SPacket.STREAM_CODEC);
        //?} else {
        /*PayloadTypeRegistry.playC2S().register(ChainBreakC2SPacket.TYPE, ChainBreakC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DecorationRemoveC2SPacket.TYPE, DecorationRemoveC2SPacket.STREAM_CODEC);
        *///?}

        ServerPlayNetworking.registerGlobalReceiver(ChainBreakC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ChainBreakC2SPacket.handle(payload, context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DecorationRemoveC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> DecorationRemoveC2SPacket.handle(context.player()));
        });

        UseBlockCallback.EVENT.register(ChainItemCallbacks::chainUseEvent);

        UseItemCallback.EVENT.register((player, level, hand) -> {
            boolean handled = ChainRaycastHelper.tryPlaceBunting(player, hand)
                    || ChainRaycastHelper.tryPlaceBanner(player, hand)
                    || ChainRaycastHelper.tryPlaceHanging(player, hand)
                    || ChainRaycastHelper.tryAdjustSlack(player, hand);
            //? if >=26.1 {
            return handled ? InteractionResult.SUCCESS : InteractionResult.PASS;
            //?} else {
            /*return handled ? InteractionResultHolder.success(player.getItemInHand(hand)) : InteractionResultHolder.pass(player.getItemInHand(hand));
            *///?}
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CommonClass.fileConfig.syncToClient(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ConnectChainCommand.register(dispatcher);
        });
    }
}
//?}
