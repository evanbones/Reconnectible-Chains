package com.evandev.connectiblechains.client;

import com.evandev.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ConnectibleChainsModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        new ClientInitializer().onInitializeClient();

        EntityRendererRegistry.register(ModEntityTypes.CHAIN_KNOT.get(), ctx -> {
            ChainKnotEntityRenderer renderer = new ChainKnotEntityRenderer(ctx);
            ClientInitializer.getInstance().setChainKnotEntityRenderer(renderer);
            return renderer;
        });
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_COLLISION.get(), ChainCollisionEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(ClientInitializer.CHAIN_KNOT, ClientInitializer::getChainKnotLayerDefinition);
    }
}