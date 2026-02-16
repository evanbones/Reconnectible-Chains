package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.CommonClass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public record ChainModel(float[] vertices, float[] uvs, float[] lightFractions) {

    public static Builder builder(int initialCapacity) {
        return new Builder(initialCapacity);
    }

    public void render(VertexConsumer buffer, PoseStack matrices, int bLight0, int bLight1, int sLight0, int sLight1) {
        Matrix4f modelMatrix = matrices.last().pose();
        Matrix3f normalMatrix = matrices.last().normal();
        int count = vertices.length / 3;
        for (int i = 0; i < count; i++) {
            float f = lightFractions[i];
            int blockLight = (int) Mth.lerp(f, (float) bLight0, (float) bLight1);
            int skyLight = (int) Mth.lerp(f, (float) sLight0, (float) sLight1);
            int light = LightTexture.pack(blockLight, skyLight);
            buffer
                    .vertex(modelMatrix, vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2])
                    .color(0.8f, 0.8f, 0.8f, 1f)
                    .uv(uvs[i * 2], uvs[i * 2 + 1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normalMatrix, 1, 1, 1)
                    .endVertex();
        }
    }

    public static class Builder {
        private final List<Float> vertices;
        private final List<Float> uvs;
        private final List<Float> lightFractions;
        private int size;
        private float currentFraction = 0f;

        public Builder(int initialCapacity) {
            vertices = new ArrayList<>(initialCapacity * 3);
            uvs = new ArrayList<>(initialCapacity * 2);
            lightFractions = new ArrayList<>(initialCapacity);
        }

        public Builder fraction(float f) {
            this.currentFraction = f;
            return this;
        }

        public Builder vertex(Vector3f v) {
            vertices.add(v.x());
            vertices.add(v.y());
            vertices.add(v.z());
            lightFractions.add(currentFraction);
            return this;
        }

        public Builder uv(float u, float v) {
            uvs.add(u);
            uvs.add(v);
            return this;
        }

        public void next() {
            size++;
        }

        public ChainModel build() {
            if (vertices.size() != size * 3) CommonClass.LOGGER.error("Wrong count of vertices");
            if (uvs.size() != size * 2) CommonClass.LOGGER.error("Wrong count of uvs");
            if (lightFractions.size() != size) CommonClass.LOGGER.error("Wrong count of light fractions");

            return new ChainModel(toFloatArray(vertices), toFloatArray(uvs), toFloatArray(lightFractions));
        }

        private float[] toFloatArray(List<Float> floats) {
            float[] array = new float[floats.size()];
            int i = 0;
            for (float f : floats) {
                array[i++] = f;
            }
            return array;
        }
    }
}