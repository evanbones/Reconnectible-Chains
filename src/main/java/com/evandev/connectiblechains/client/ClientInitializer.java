package com.evandev.connectiblechains.client;

import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.CrossCatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.PlusCatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.catenary.SquareCatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.evandev.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import java.util.Optional;

public class ClientInitializer {

    public static final ModelLayerLocation CHAIN_KNOT = new ModelLayerLocation(MathHelper.identifier("chain_knot"), "main");
    private static ClientInitializer instance;
    private final ChainTextureManager chainTextureManager = new ChainTextureManager();
    private ChainKnotEntityRenderer chainKnotEntityRenderer;

    public static LayerDefinition getChainKnotLayerDefinition() {
        return ChainKnotEntityModel.getTexturedModelData();
    }

    public static ClientInitializer getInstance() {
        return instance;
    }

    public void onInitializeClient() {
        instance = this;
        registerCatenaryRenders();
    }

    private void registerCatenaryRenders() {
        CatenaryRenderer.addRenderer(MathHelper.identifier("cross"), CrossCatenaryRenderer::new);
        CatenaryRenderer.addRenderer(MathHelper.identifier("square"), SquareCatenaryRenderer::new);
        CatenaryRenderer.addRenderer(MathHelper.identifier("plus"), PlusCatenaryRenderer::new);
    }

    public Optional<ChainKnotEntityRenderer> getChainKnotEntityRenderer() {
        return Optional.ofNullable(chainKnotEntityRenderer);
    }

    public void setChainKnotEntityRenderer(ChainKnotEntityRenderer renderer) {
        this.chainKnotEntityRenderer = renderer;
    }

    public ChainTextureManager getChainTextureManager() {
        return chainTextureManager;
    }
}