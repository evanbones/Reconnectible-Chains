/*
 * Copyright (C) 2024 legoatoom.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.evandev.connectiblechains.client;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.CrossCatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.PlussCatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.SquareCatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.evandev.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.evandev.connectiblechains.config.ModConfig;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.util.Helper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;

import java.util.Optional;

/**
 * ClientInitializer.
 * This method is called when the game starts with a client.
 * This registers the renderers for entities and how to handle packages between the server and client.
 *
 * @author legoatoom
 */
@Environment(EnvType.CLIENT)
public class ClientInitializer implements ClientModInitializer {

    public static final EntityModelLayer CHAIN_KNOT = new EntityModelLayer(Helper.identifier("chain_knot"), "main");
    private static ClientInitializer instance;
    private final ChainTextureManager chainTextureManager = new ChainTextureManager();
    private ChainKnotEntityRenderer chainKnotEntityRenderer;


    @Override
    public void onInitializeClient() {
        instance = this;
        initRenderers();

        registerNetworkEventHandlers();
        registerClientEventHandlers();

        registerConfigSync();

        // Tooltip for chains.
        ItemTooltipCallback.EVENT.register(ChainItemCallbacks::infoToolTip);

        registerCatenaryRenders();
    }

    private void registerCatenaryRenders() {
        CatenaryRenderer.addRenderer(Helper.identifier("cross"), CrossCatenaryRenderer::new);
        CatenaryRenderer.addRenderer(Helper.identifier("square"), SquareCatenaryRenderer::new);
        CatenaryRenderer.addRenderer(Helper.identifier("plus"), PlussCatenaryRenderer::new);
    }

    private static void registerConfigSync() {
        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        configHolder.registerSaveListener((holder, modConfig) -> {
            ClientInitializer clientInitializer = ClientInitializer.getInstance();

            if (clientInitializer != null) {
                clientInitializer.getChainKnotEntityRenderer().ifPresent(renderer -> renderer.getChainRenderer().purge());
            }
            MinecraftServer server = MinecraftClient.getInstance().getServer();
            if (server != null) {
                CommonClass.LOGGER.info("Syncing config to clients");
                CommonClass.fileConfig.syncToClients(server);
                CommonClass.runtimeConfig.copyFrom(CommonClass.fileConfig);
            }
            return ActionResult.PASS;
        });
    }

    private void initRenderers() {
        CommonClass.LOGGER.info("Initializing Renderers.");
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_KNOT, ctx -> {
            chainKnotEntityRenderer = new ChainKnotEntityRenderer(ctx);
            return chainKnotEntityRenderer;
        });
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_COLLISION, ChainCollisionEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(CHAIN_KNOT, ChainKnotEntityModel::getTexturedModelData);
    }

    private void registerNetworkEventHandlers() {
        CommonClass.LOGGER.info("Initializing Network even handlers.");
        ClientPlayNetworking.registerGlobalReceiver(ChainAttachS2CPacket.TYPE, ChainAttachS2CPacket::apply);
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, ConfigSyncPayload::apply);

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            // Load client config
            CommonClass.runtimeConfig.copyFrom(CommonClass.fileConfig);
            getChainKnotEntityRenderer().ifPresent(r -> r.getChainRenderer().purge());
        });

    }

    private void registerClientEventHandlers() {
        CommonClass.LOGGER.info("Registering texture handlers..");

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(chainTextureManager);
    }

    public static ClientInitializer getInstance() {
        return instance;
    }

    public Optional<ChainKnotEntityRenderer> getChainKnotEntityRenderer() {
        return Optional.ofNullable(chainKnotEntityRenderer);
    }

    public ChainTextureManager getChainTextureManager() {
        return chainTextureManager;
    }
}
