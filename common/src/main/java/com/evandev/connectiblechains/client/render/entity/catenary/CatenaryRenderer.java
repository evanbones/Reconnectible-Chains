package com.evandev.connectiblechains.client.render.entity.catenary;

import com.evandev.connectiblechains.client.render.entity.model.ChainModel;
import com.evandev.connectiblechains.client.render.entity.UVRect;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.function.BiFunction;

public abstract class CatenaryRenderer {

    protected static final float CHAIN_SCALE = 1f;
    protected static final int MAX_SEGMENTS = 2048;

    private static final HashMap<ResourceLocation, BiFunction<UVRect, UVRect, CatenaryRenderer>> renderers = new HashMap<>();
    protected final UVRect SIDE_A;
    protected final UVRect SIDE_B;

    protected CatenaryRenderer(UVRect a, UVRect b) {
        SIDE_A = a;
        SIDE_B = b;
    }

    public static void addRenderer(ResourceLocation id, BiFunction<UVRect, UVRect, CatenaryRenderer> rendererSupplier) {
        renderers.put(id, rendererSupplier);
    }

    public static CatenaryRenderer getRenderer(ResourceLocation id, Pair<UVRect, UVRect> uvRects) {
        return renderers.getOrDefault(id, CrossCatenaryRenderer::new).apply(uvRects.getFirst(), uvRects.getSecond());
    }

    public UVRect getSideA() {
        return SIDE_A;
    }

    public UVRect getSideB() {
        return SIDE_B;
    }

    public abstract ChainModel buildModel(Vector3f chainVec, float slack);

    protected float estimateDeltaX(float s, float k) {
        return (float) (s / Math.sqrt(1 + k * k));
    }
}