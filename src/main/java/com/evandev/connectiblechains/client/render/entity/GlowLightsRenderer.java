package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.CommonClass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Vector3f;

//? if >=26.1 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GlowLightsRenderer {
    private static final String ITEM_PREFIX = "glow_lights_";
    private static final float HALF_WIDTH = 2.1f / 16f;
    private static final float END_INSET = 2f / 16f;
    private static final float FACE_U_WIDTH = 0.25f;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int FRAME_TIME = 9;

    private final Map<Identifier, Optional<Sleeve>> sleeves = new HashMap<>();
    private final Vector3f[] corners0 = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    private final Vector3f[] corners1 = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

    public Optional<Sleeve> sleeve(Identifier glowLights) {
        return sleeves.computeIfAbsent(glowLights, GlowLightsRenderer::loadSleeve);
    }

    public void clear() {
        sleeves.clear();
    }

    private static Optional<Sleeve> loadSleeve(Identifier glowLights) {
        String path = glowLights.getPath();
        if (!path.startsWith(ITEM_PREFIX)) return Optional.empty();

        String variant = path.substring(ITEM_PREFIX.length());
        if (variant.equals("prismatic")) variant = "rainbow";
        Identifier texture = Identifier.fromNamespaceAndPath(glowLights.getNamespace(), "textures/block/roped_lights_" + variant + ".png");

        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return Optional.empty();

        //? if >=26.1 {
        RenderType renderType = RenderTypes.entityCutout(texture);
        //?} else {
        /*RenderType renderType = RenderType.entityCutoutNoCull(texture);
        *///?}
        return Optional.of(new Sleeve(renderType, frameCount(resource.get())));
    }

    private static int frameCount(Resource resource) {
        try (InputStream in = resource.open()) {
            byte[] header = in.readNBytes(24);
            if (header.length < 24) return 1;
            int width = readInt(header, 16);
            int height = readInt(header, 20);
            return width > 0 ? Math.max(1, height / width) : 1;
        } catch (IOException e) {
            return 1;
        }
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16) | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }

    public void render(PoseStack.Pose pose, VertexConsumer vc, ChainKnotEntityRenderer.ChainCurve curve, Sleeve sleeve, long gameTime) {
        float frameHeight = 1f / sleeve.frames();
        float frameOffset = (int) ((gameTime / FRAME_TIME) % sleeve.frames()) * frameHeight;

        float length = curve.distanceXZ();
        float inset = Math.min(END_INSET, length * 0.25f);
        float xEnd = length - inset;
        float step = 1f / CommonClass.runtimeConfig.getQuality();

        float x0 = inset;
        float y0 = curve.y(x0);
        float slope0 = curve.slope(x0);
        float v = 0f;
        int cell = 0;

        while (x0 < xEnd) {
            float x1 = Math.min(xEnd, x0 + step / (float) Math.sqrt(1 + slope0 * slope0));
            float y1 = curve.y(x1);
            float slope1 = curve.slope(x1);
            float segLen = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));

            if (segLen > 1e-5f) {
                float sFrom = 0f;
                while (sFrom < 1f) {
                    float sBoundary = (cell + 1 - v) / segLen;
                    float sTo = Math.min(1f, sBoundary);
                    if (sTo > sFrom) {
                        float vFrom = clamp01(v + sFrom * segLen - cell);
                        float vTo = clamp01(v + sTo * segLen - cell);
                        corners(corners0, x0, y0, slope0, x1, y1, slope1, sFrom);
                        corners(corners1, x0, y0, slope0, x1, y1, slope1, sTo);
                        emitSleeve(pose, vc, frameOffset + vFrom * frameHeight, frameOffset + vTo * frameHeight);
                    }
                    if (sBoundary <= 1f) cell++;
                    sFrom = Math.max(sFrom, sTo);
                }
                v += segLen;
            }

            x0 = x1;
            y0 = y1;
            slope0 = slope1;
        }
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static void corners(Vector3f[] out, float x0, float y0, float slope0, float x1, float y1, float slope1, float s) {
        float x = x0 + (x1 - x0) * s;
        float y = y0 + (y1 - y0) * s;
        float slope = slope0 + (slope1 - slope0) * s;
        float invLen = 1f / (float) Math.sqrt(1 + slope * slope);
        float nx = -slope * invLen * HALF_WIDTH;
        float ny = invLen * HALF_WIDTH;

        out[0].set(x + nx, y + ny, -HALF_WIDTH);
        out[1].set(x + nx, y + ny, HALF_WIDTH);
        out[2].set(x - nx, y - ny, HALF_WIDTH);
        out[3].set(x - nx, y - ny, -HALF_WIDTH);
    }

    private void emitSleeve(PoseStack.Pose pose, VertexConsumer vc, float v0, float v1) {
        for (int face = 0; face < 4; face++) {
            Vector3f a0 = corners0[face], b0 = corners0[(face + 1) % 4];
            Vector3f a1 = corners1[face], b1 = corners1[(face + 1) % 4];
            float nx = (a0.x + b0.x) * 0.5f - (corners0[(face + 2) % 4].x + corners0[(face + 3) % 4].x) * 0.5f;
            float ny = (a0.y + b0.y) * 0.5f - (corners0[(face + 2) % 4].y + corners0[(face + 3) % 4].y) * 0.5f;
            float nz = (a0.z + b0.z) * 0.5f - (corners0[(face + 2) % 4].z + corners0[(face + 3) % 4].z) * 0.5f;
            float invLen = 1f / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx *= invLen;
            ny *= invLen;
            nz *= invLen;

            float u0 = face * FACE_U_WIDTH;
            float u1 = u0 + FACE_U_WIDTH;
            vertex(vc, pose, a0, u0, v0, nx, ny, nz);
            vertex(vc, pose, a1, u0, v1, nx, ny, nz);
            vertex(vc, pose, b1, u1, v1, nx, ny, nz);
            vertex(vc, pose, b0, u1, v0, nx, ny, nz);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f p, float u, float v, float nx, float ny, float nz) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    public record Sleeve(RenderType renderType, int frames) {
    }
}
