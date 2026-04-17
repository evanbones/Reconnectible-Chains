package com.evandev.connectiblechains.client.render.entity.catenary;

import com.evandev.connectiblechains.client.render.entity.UVRect;
import com.evandev.connectiblechains.client.render.entity.model.ChainModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.function.BiFunction;

public abstract class CatenaryRenderer {

    protected static final float CHAIN_SCALE = 1f;
    protected static final int MAX_SEGMENTS = 2048;

    private static final HashMap<Identifier, BiFunction<UVRect, UVRect, CatenaryRenderer>> renderers = new HashMap<>();
    protected final UVRect SIDE_A;
    protected final UVRect SIDE_B;

    protected CatenaryRenderer(UVRect a, UVRect b) {
        SIDE_A = a;
        SIDE_B = b;
    }

    public static void addRenderer(Identifier id, BiFunction<UVRect, UVRect, CatenaryRenderer> rendererSupplier) {
        renderers.put(id, rendererSupplier);
    }

    public static CatenaryRenderer getRenderer(Identifier id, Pair<UVRect, UVRect> uvRects) {
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

    protected void addQuad(ChainModel.Builder builder, float f0, float f1, float u0, float u1, float v0, float v1, Vector3f p00, Vector3f p01, Vector3f p11, Vector3f p10) {
        Vector3f w = new Vector3f(p01).sub(p00);
        Vector3f l = new Vector3f(p10).sub(p00);
        Vector3f normal = w.cross(l).normalize();

        builder.fraction(f0).normal(normal).vertex(p00).uv(u0, v0).next();
        builder.fraction(f0).normal(normal).vertex(p01).uv(u1, v0).next();
        builder.fraction(f1).normal(normal).vertex(p11).uv(u1, v1).next();
        builder.fraction(f1).normal(normal).vertex(p10).uv(u0, v1).next();
    }

    protected void addDoubleSidedQuad(ChainModel.Builder builder, float f0, float f1, float u0, float u1, float v0, float v1, Vector3f p00, Vector3f p01, Vector3f p11, Vector3f p10) {
        addQuad(builder, f0, f1, u0, u1, v0, v1, p00, p01, p11, p10);
        addQuad(builder, f0, f1, u1, u0, v0, v1, p01, p00, p10, p11);
    }
}