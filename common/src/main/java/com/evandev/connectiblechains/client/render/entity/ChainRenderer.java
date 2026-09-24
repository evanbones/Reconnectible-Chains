package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.model.ChainModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ChainRenderer {

    private static final int MAX_CACHE_SIZE = 1000;

    private final Map<BakeKey, ChainModel> models = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BakeKey, ChainModel> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private final BakeKey lookupKey = new BakeKey();

    public void renderBaked(CatenaryRenderer renderer, VertexConsumer buffer, PoseStack matrices, Vector3f chainVec, float slack, int blockLight0, int blockLight1, int skyLight0, int skyLight1, int tintColor) {
        if (!isFinite(chainVec)) return;
        lookupKey.set(chainVec, renderer, slack);

        ChainModel model = models.get(lookupKey);
        if (model == null) {
            model = renderer.buildModel(chainVec, slack);
            models.put(new BakeKey().set(chainVec, renderer, slack), model);
        }
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1, tintColor);
    }

    public void render(CatenaryRenderer renderer, VertexConsumer buffer, PoseStack matrices, Vector3f chainVec, float slack, int blockLight0, int blockLight1, int skyLight0, int skyLight1, int tintColor) {
        if (!isFinite(chainVec)) return;
        ChainModel model = renderer.buildModel(chainVec, slack);
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1, tintColor);
    }

    private static boolean isFinite(Vector3f v) {
        return Float.isFinite(v.x + v.y + v.z);
    }

    public void purge() {
        models.clear();
    }

    public static class BakeKey {
        private final Vector3f chainVec = new Vector3f();
        private Class<? extends CatenaryRenderer> rendererClass;
        private UVRect sideA;
        private UVRect sideB;
        private float slack;
        private int hash;

        public BakeKey set(Vector3f chainVec, CatenaryRenderer renderer, float slack) {
            this.chainVec.set(chainVec);
            this.rendererClass = renderer.getClass();
            this.sideA = renderer.getSideA();
            this.sideB = renderer.getSideB();
            this.slack = slack;

            int h = chainVec.hashCode();
            h = 31 * h + rendererClass.hashCode();
            h = 31 * h + sideA.hashCode();
            h = 31 * h + sideB.hashCode();
            this.hash = 31 * h + Float.floatToIntBits(slack);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BakeKey bakeKey)) return false;
            return Float.compare(bakeKey.slack, slack) == 0 &&
                    chainVec.equals(bakeKey.chainVec) &&
                    rendererClass == bakeKey.rendererClass &&
                    Objects.equals(sideA, bakeKey.sideA) &&
                    Objects.equals(sideB, bakeKey.sideB);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}