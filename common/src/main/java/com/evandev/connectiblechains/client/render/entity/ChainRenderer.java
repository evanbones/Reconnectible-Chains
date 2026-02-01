package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
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

    public void renderBaked(CatenaryRenderer renderer, VertexConsumer buffer, PoseStack matrices, BakeKey ignoredOldKey, Vector3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        BakeKey key = new BakeKey(chainVec);

        ChainModel model;
        if (models.containsKey(key)) {
            model = models.get(key);
        } else {
            model = renderer.buildModel(chainVec);
            models.put(key, model);
        }

        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    public void render(CatenaryRenderer renderer, VertexConsumer buffer, PoseStack matrices, Vector3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel model = renderer.buildModel(chainVec);
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    public void purge() {
        models.clear();
    }

    public static class BakeKey {
        private final Vector3f chainVec;

        public BakeKey(Vector3f chainVec) {
            this.chainVec = new Vector3f(chainVec);
        }

        public BakeKey(Vec3 srcPos, Vec3 dstPos) {
            this.chainVec = new Vector3f((float) (dstPos.x - srcPos.x), (float) (dstPos.y - srcPos.y), (float) (dstPos.z - srcPos.z));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BakeKey bakeKey)) return false;
            return Objects.equals(chainVec, bakeKey.chainVec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chainVec);
        }
    }
}