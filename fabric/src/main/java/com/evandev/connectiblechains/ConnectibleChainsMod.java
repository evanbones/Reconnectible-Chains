package com.evandev.connectiblechains;

import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.platform.FabricNetworkHelper;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResultHolder;

public class ConnectibleChainsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        FabricNetworkHelper.init();
        CommonClass.init();

        UseBlockCallback.EVENT.register(ChainItemCallbacks::chainUseEvent);

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (ChainRaycastHelper.tryAdjustSlack(player, hand)) {
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CommonClass.fileConfig.syncToClient(handler.getPlayer()));
    }
}