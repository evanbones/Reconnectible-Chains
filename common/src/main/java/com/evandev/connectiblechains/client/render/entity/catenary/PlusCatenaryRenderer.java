package com.evandev.connectiblechains.client.render.entity.catenary;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.render.entity.ChainModel;
import com.evandev.connectiblechains.client.render.entity.UVRect;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.evandev.connectiblechains.util.MathHelper.drip2;
import static com.evandev.connectiblechains.util.MathHelper.drip2prime;

public class PlusCatenaryRenderer extends CatenaryRenderer {

    public PlusCatenaryRenderer(UVRect a, UVRect b) {
        super(a, b);
    }

    @Override
    public ChainModel buildModel(Vector3f chainVec, float slack) {
        float desiredSegmentLength = 1f / CommonClass.runtimeConfig.getQuality();
        int initialCapacity = (int) (2f * chainVec.length() / desiredSegmentLength);
        ChainModel.Builder builder = ChainModel.builder(initialCapacity);

        if (chainVec.x() == 0F && chainVec.z() == 0F) {
            buildFaceVertical(builder, chainVec, 0, SIDE_A);
            buildFaceVertical(builder, chainVec, 90, SIDE_B);
        } else {
            buildFace(builder, chainVec, 0, SIDE_A, slack);
            buildFace(builder, chainVec, 90, SIDE_B, slack);
        }

        return builder.build();
    }

    private void buildFaceVertical(ChainModel.Builder builder, Vector3f endPosition, float angle, UVRect uv) {
        endPosition.x = 0F;
        endPosition.z = 0F;
        final float chainWidth = (uv.x1() - uv.x0()) / 16F * CHAIN_SCALE;

        Vector3f normal = new Vector3f((float) Math.cos(Math.toRadians(angle)), 0F, (float) Math.sin(Math.toRadians(angle)));
        normal.normalize(chainWidth / 2);

        Vector3f vert00 = new Vector3f(-normal.x(), 0, -normal.z());
        Vector3f vert01 = new Vector3f(normal.x(), 0, normal.z());
        Vector3f vert10 = new Vector3f(-normal.x(), endPosition.y(), -normal.z());
        Vector3f vert11 = new Vector3f(normal.x(), endPosition.y(), normal.z());

        float f0 = 0F, f1 = 1F;
        float uvv0 = 0F, uvv1 = Math.abs(endPosition.y()) / CHAIN_SCALE;
        builder.fraction(f0).vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
        builder.fraction(f0).vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
        builder.fraction(f1).vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
        builder.fraction(f1).vertex(vert10).uv(uv.x0() / 16f, uvv1).next();
    }

    private void buildFace(ChainModel.Builder builder, Vector3f endPosition, float angle, UVRect uv, float slack) {
        float desiredSegmentLength = 1f / CommonClass.runtimeConfig.getQuality();
        float distance = endPosition.length();
        float distanceXZ = (float) Math.sqrt(Math.fma(endPosition.x(), endPosition.x(), endPosition.z() * endPosition.z()));
        final float wrongDistanceFactor = distance / distanceXZ;
        final float chainWidth = (uv.x1() - uv.x0()) / 16F * CHAIN_SCALE;
        Vector3f normal = new Vector3f(), rotAxis = new Vector3f();
        Vector3f vert00 = new Vector3f();
        Vector3f vert01 = new Vector3f();
        Vector3f vert11 = new Vector3f();
        Vector3f vert10 = new Vector3f();
        Quaternionf rotator = new Quaternionf();
        Vector3f segmentStart = new Vector3f(), segmentEnd = new Vector3f();

        float uvv1 = 0;
        float uvv0;
        float x = 0;
        float f0, f1 = 0;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            float gradient = (float) drip2prime(x * wrongDistanceFactor, distance, endPosition.y(), slack);
            x += estimateDeltaX(desiredSegmentLength, gradient);
            x = Math.min(x, distanceXZ);

            f0 = f1;
            f1 = x / distanceXZ;

            float y = (float) drip2(x * wrongDistanceFactor, distance, endPosition.y(), slack);
            segmentEnd.set(x, y, 0);

            rotAxis.set(segmentEnd.x() - segmentStart.x(), segmentEnd.y() - segmentStart.y(), segmentEnd.z() - segmentStart.z());
            rotAxis.normalize();
            rotator = rotator.fromAxisAngleDeg(rotAxis, angle);

            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.rotate(rotator);
            normal.normalize(chainWidth / 2);

            if (segment == 0) {
                vert00.set(segmentStart).sub(normal);
                vert01.set(segmentStart).add(normal);
            } else {
                vert00.set(vert10);
                vert01.set(vert11);
            }
            vert10.set(segmentEnd).sub(normal);
            vert11.set(segmentEnd).add(normal);

            float actualSegmentLength = segmentStart.distance(segmentEnd);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / CHAIN_SCALE;

            builder.fraction(f0).vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
            builder.fraction(f0).vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
            builder.fraction(f1).vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
            builder.fraction(f1).vertex(vert10).uv(uv.x0() / 16f, uvv1).next();

            if (x >= distanceXZ) {
                break;
            }
            segmentStart.set(segmentEnd);
        }
    }
}