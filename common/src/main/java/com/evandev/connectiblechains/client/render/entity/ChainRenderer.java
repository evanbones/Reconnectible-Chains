package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
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

    public void renderBaked(CatenaryRenderer renderer, VertexConsumer buffer, PoseStack matrices, Vector3f chainVec, float slack, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        BakeKey key = new BakeKey(chainVec, renderer, slack);

        ChainModel model = models.computeIfAbsent(key, k -> renderer.buildModel(chainVec, slack));
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    public void render(CatenaryRenderer renderer, VertexConsumer buffer, PoseStack matrices, Vector3f chainVec, float slack, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel model = renderer.buildModel(chainVec, slack);
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    public void purge() {
        models.clear();
    }

    public static class BakeKey {
        private final Vector3f chainVec;
        private final Class<? extends CatenaryRenderer> rendererClass;
        private final UVRect sideA;
        private final UVRect sideB;
        private final float slack;

        public BakeKey(Vector3f chainVec, CatenaryRenderer renderer, float slack) {
            this.chainVec = new Vector3f(chainVec);
            this.rendererClass = renderer.getClass();
            this.sideA = renderer.getSideA();
            this.sideB = renderer.getSideB();
            this.slack = slack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BakeKey bakeKey)) return false;
            return Float.compare(bakeKey.slack, slack) == 0 &&
                    Objects.equals(chainVec, bakeKey.chainVec) &&
                    Objects.equals(rendererClass, bakeKey.rendererClass) &&
                    Objects.equals(sideA, bakeKey.sideA) &&
                    Objects.equals(sideB, bakeKey.sideB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chainVec, rendererClass, sideA, sideB, slack);
        }
    }
}