package com.evandev.connectiblechains.client.render.entity.model;

import com.evandev.connectiblechains.CommonClass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record ChainModel(float[] vertices, float[] uvs, float[] lightFractions, float[] normals, boolean shaded) {

    // Matches the 25 vertex pairs vanilla interpolates a leash over.
    private static final int LIGHT_STEPS = 25;

    public static Builder builder(int initialCapacity, boolean shaded) {
        return new Builder(initialCapacity, shaded);
    }

    private static int[] packLightRamp(int bLight0, int bLight1, int sLight0, int sLight1) {
        int[] ramp = new int[LIGHT_STEPS];
        for (int i = 0; i < LIGHT_STEPS; i++) {
            float f = (float) i / (LIGHT_STEPS - 1);
            ramp[i] = LightCoordsUtil.pack(
                    (int) Mth.lerp(f, (float) bLight0, (float) bLight1),
                    (int) Mth.lerp(f, (float) sLight0, (float) sLight1));
        }
        return ramp;
    }

    public void render(VertexConsumer buffer, PoseStack matrices, int bLight0, int bLight1, int sLight0, int sLight1, int tintColor) {
        Matrix4f m = matrices.last().pose();
        Matrix3f normalMatrix = matrices.last().normal();
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float g = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;
        float a = ((tintColor >> 24) & 0xFF) / 255.0f;

        int[] lightRamp = packLightRamp(bLight0, bLight1, sLight0, sLight1);

        int count = vertices.length / 3;
        Vector3f norm = new Vector3f();

        for (int i = 0; i < count; i++) {
            float x = vertices[i * 3];
            float y = vertices[i * 3 + 1];
            float z = vertices[i * 3 + 2];

            float tx = Math.fma(m.m00(), x, Math.fma(m.m10(), y, Math.fma(m.m20(), z, m.m30())));
            float ty = Math.fma(m.m01(), x, Math.fma(m.m11(), y, Math.fma(m.m21(), z, m.m31())));
            float tz = Math.fma(m.m02(), x, Math.fma(m.m12(), y, Math.fma(m.m22(), z, m.m32())));

            norm.set(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
            if (shaded) {
                normalMatrix.transform(norm);
            }

            int step = Mth.clamp((int) (lightFractions[i] * (LIGHT_STEPS - 1) + 0.5f), 0, LIGHT_STEPS - 1);

            buffer
                    .addVertex(tx, ty, tz)
                    .setColor(r, g, b, a)
                    .setUv(uvs[i * 2], uvs[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(lightRamp[step])
                    .setNormal(norm.x(), norm.y(), norm.z());

            if (CommonClass.runtimeConfig.doDebugDraw()) {
                buffer.setLineWidth(1.0f);
            }
        }
    }

    public static class Builder {
        private final FloatArrayList vertices;
        private final FloatArrayList uvs;
        private final FloatArrayList lightFractions;
        private final FloatArrayList normals;
        private final boolean shaded;
        private int size;
        private float currentFraction = 0f;

        public Builder(int initialCapacity, boolean shaded) {
            this.shaded = shaded;
            int safeCapacity = Math.min(Math.max(initialCapacity, 16), 65536);
            vertices = new FloatArrayList(safeCapacity * 3);
            uvs = new FloatArrayList(safeCapacity * 2);
            lightFractions = new FloatArrayList(safeCapacity);
            normals = new FloatArrayList(safeCapacity * 3);
        }

        public Builder fraction(float f) {
            this.currentFraction = f;
            return this;
        }

        public Builder normal(Vector3f n) {
            normals.add(n.x());
            normals.add(n.y());
            normals.add(n.z());
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

            return new ChainModel(vertices.toFloatArray(), uvs.toFloatArray(), lightFractions.toFloatArray(), normals.toFloatArray(), shaded);
        }
    }
}
