package com.evandev.connectiblechains.client.render.entity.model;

import com.evandev.connectiblechains.CommonClass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
//? if >=26.1 {
import net.minecraft.util.LightCoordsUtil;
//?} else {
/*import net.minecraft.client.renderer.LightTexture;
*///?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ChainModel {

    private static final int LIGHT_STEPS = 25;

    private final float[] vertices;
    private final float[] uvs;
    private final float[] normals;
    private final byte[] lightSteps;
    private final boolean shaded;

    private ChainModel(float[] vertices, float[] uvs, float[] normals, byte[] lightSteps, boolean shaded) {
        this.vertices = vertices;
        this.uvs = uvs;
        this.normals = normals;
        this.lightSteps = lightSteps;
        this.shaded = shaded;
    }

    public static Builder builder(int initialCapacity, boolean shaded) {
        return new Builder(initialCapacity, shaded);
    }

    private static int packLight(int blockLight, int skyLight) {
//? if >=26.1 {
        return LightCoordsUtil.pack(blockLight, skyLight);
//?} else {
        /*return LightTexture.pack(blockLight, skyLight);
*///?}
    }

    private static int[] buildLightRamp(int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        int[] ramp = new int[LIGHT_STEPS];
        for (int i = 0; i < LIGHT_STEPS; i++) {
            float f = (float) i / (LIGHT_STEPS - 1);
            ramp[i] = packLight(
                    (int) Mth.lerp(f, (float) blockLight0, (float) blockLight1),
                    (int) Mth.lerp(f, (float) skyLight0, (float) skyLight1));
        }
        return ramp;
    }

    /**
     * Rotates the baked geometry around Y so the mesh already points along the world space chain
     * vector. Callers then only have to translate.
     */
    public void rotateY(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i];
            float z = vertices[i + 2];
            vertices[i] = x * cos + z * sin;
            vertices[i + 2] = -x * sin + z * cos;

            float nx = normals[i];
            float nz = normals[i + 2];
            normals[i] = nx * cos + nz * sin;
            normals[i + 2] = -nx * sin + nz * cos;
        }
    }

    public void render(VertexConsumer buffer, PoseStack.Pose pose, int tintColor, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float g = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;
        float a = ((tintColor >> 24) & 0xFF) / 255.0f;
        boolean debugLines = CommonClass.runtimeConfig.doDebugDraw();

        boolean uniformLight = blockLight0 == blockLight1 && skyLight0 == skyLight1;
        int uniformLightValue = uniformLight ? packLight(blockLight0, skyLight0) : 0;
        int[] lightRamp = uniformLight ? null : buildLightRamp(blockLight0, blockLight1, skyLight0, skyLight1);

        int count = vertices.length / 3;
        if (isTranslationOnly(matrix)) {
            float tx = matrix.m30();
            float ty = matrix.m31();
            float tz = matrix.m32();
            for (int i = 0; i < count; i++) {
                int v = i * 3;
                int light = uniformLight ? uniformLightValue : lightRamp[lightSteps[i] & 0xFF];

                buffer
                        .addVertex(vertices[v] + tx, vertices[v + 1] + ty, vertices[v + 2] + tz)
                        .setColor(r, g, b, a)
                        .setUv(uvs[i * 2], uvs[i * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(normals[v], normals[v + 1], normals[v + 2]);

                //? if >=26.1 {
                if (debugLines) {
                    buffer.setLineWidth(1.0f);
                }
                //?}
            }
            return;
        }

        for (int i = 0; i < count; i++) {
            int v = i * 3;
            float x = vertices[v];
            float y = vertices[v + 1];
            float z = vertices[v + 2];

            float px = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
            float py = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
            float pz = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));

            float nx = normals[v];
            float ny = normals[v + 1];
            float nz = normals[v + 2];
            if (shaded) {
                float tnx = normalMatrix.m00() * nx + normalMatrix.m01() * ny + normalMatrix.m02() * nz;
                float tny = normalMatrix.m10() * nx + normalMatrix.m11() * ny + normalMatrix.m12() * nz;
                float tnz = normalMatrix.m20() * nx + normalMatrix.m21() * ny + normalMatrix.m22() * nz;
                nx = tnx;
                ny = tny;
                nz = tnz;
            }

            int light = uniformLight ? uniformLightValue : lightRamp[lightSteps[i] & 0xFF];

            buffer
                    .addVertex(px, py, pz)
                    .setColor(r, g, b, a)
                    .setUv(uvs[i * 2], uvs[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(nx, ny, nz);

            //? if >=26.1 {
            if (debugLines) {
                buffer.setLineWidth(1.0f);
            }
            //?}
        }
    }

    private static boolean isTranslationOnly(Matrix4f m) {
        return near(m.m00(), 1.0f) && near(m.m01(), 0.0f) && near(m.m02(), 0.0f) && near(m.m03(), 0.0f)
                && near(m.m10(), 0.0f) && near(m.m11(), 1.0f) && near(m.m12(), 0.0f) && near(m.m13(), 0.0f)
                && near(m.m20(), 0.0f) && near(m.m21(), 0.0f) && near(m.m22(), 1.0f) && near(m.m23(), 0.0f)
                && near(m.m33(), 1.0f);
    }

    private static boolean near(float value, float expected) {
        return Math.abs(value - expected) < 1.0E-5f;
    }

    private static int lightStep(float fraction) {
        return Mth.clamp((int) (fraction * (LIGHT_STEPS - 1) + 0.5f), 0, LIGHT_STEPS - 1);
    }

    public static final class Builder {
        private final FloatArrayList vertices;
        private final FloatArrayList uvs;
        private final FloatArrayList normals;
        private final ByteArrayList lightSteps;
        private final boolean shaded;
        private int size;
        private float currentFraction = 0f;

        public Builder(int initialCapacity, boolean shaded) {
            this.shaded = shaded;
            int safeCapacity = Math.min(Math.max(initialCapacity, 16), 65536);
            vertices = new FloatArrayList(safeCapacity * 3);
            uvs = new FloatArrayList(safeCapacity * 2);
            normals = new FloatArrayList(safeCapacity * 3);
            lightSteps = new ByteArrayList(safeCapacity);
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
            lightSteps.add((byte) lightStep(currentFraction));
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
            if (lightSteps.size() != size) CommonClass.LOGGER.error("Wrong count of light steps");

            return new ChainModel(
                    vertices.toFloatArray(),
                    uvs.toFloatArray(),
                    normals.toFloatArray(),
                    lightSteps.toByteArray(),
                    shaded);
        }
    }
}
