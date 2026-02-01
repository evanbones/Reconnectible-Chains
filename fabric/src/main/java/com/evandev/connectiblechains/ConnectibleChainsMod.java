package com.evandev.connectiblechains;

import com.evandev.connectiblechains.item.ChainItemCallbacks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ConnectibleChainsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        UseBlockCallback.EVENT.register(ChainItemCallbacks::chainUseEvent);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CommonClass.fileConfig.syncToClient(handler.getPlayer()));
    }
}