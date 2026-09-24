package com.evandev.connectiblechains.client.render.entity.catenary;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.render.entity.UVRect;
import com.evandev.connectiblechains.client.render.entity.model.ChainModel;
import com.evandev.connectiblechains.util.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SquareCatenaryRenderer extends CatenaryRenderer {
    public static final float SQRT_2 = (float) Math.sqrt(2);
    protected static final float CHAIN_SCALE = 1F;
    private static final float KNOT_INSET = 0.0625F;

    public SquareCatenaryRenderer(UVRect a, UVRect b) {
        super(a, b);
    }

    @Override
    public ChainModel buildModel(Vector3f chainVec, float slack) {
        ChainModel.Builder builder = createBuilder(chainVec);

        if (chainVec.x() == 0F && chainVec.z() == 0F) {
            buildFaceVertical(builder, chainVec);
        } else {
            buildFace(builder, chainVec, slack);
        }

        return builder.build();
    }

    private void buildFaceVertical(ChainModel.Builder builder, Vector3f endPosition) {
        endPosition.x = 0F;
        endPosition.z = 0F;
        final float chainHalfWidthA = (SIDE_A.x1() - SIDE_A.x0()) / 32F * CHAIN_SCALE;
        final float chainHalfWidthB = (SIDE_B.x1() - SIDE_B.x0()) / 32F * CHAIN_SCALE;

        Vector3f normalA = new Vector3f((float) Math.cos(Math.toRadians(45)), 0F, (float) Math.sin(Math.toRadians(45)));
        Vector3f normalB = new Vector3f((float) Math.cos(Math.toRadians(-45)), 0F, (float) Math.sin(Math.toRadians(-45)));
        normalA.normalize(chainHalfWidthA * SQRT_2);
        normalB.normalize(chainHalfWidthB * SQRT_2);

        float length = Math.abs(endPosition.y());
        float inset = Math.min(KNOT_INSET, length * 0.25F);
        float sign = Math.signum(endPosition.y());
        float yStart = sign * inset;
        float yEnd = endPosition.y() - sign * inset;

        Vector3f vert00A = new Vector3f(-normalA.x(), yStart, -normalA.z());
        Vector3f vert01A = new Vector3f(normalA.x(), yStart, normalA.z());
        Vector3f vert10A = new Vector3f(-normalA.x(), yEnd, -normalA.z());
        Vector3f vert11A = new Vector3f(normalA.x(), yEnd, normalA.z());

        Vector3f vert00B = new Vector3f(-normalB.x(), yStart, -normalB.z());
        Vector3f vert01B = new Vector3f(normalB.x(), yStart, normalB.z());
        Vector3f vert10B = new Vector3f(-normalB.x(), yEnd, -normalB.z());
        Vector3f vert11B = new Vector3f(normalB.x(), yEnd, normalB.z());

        float f0 = inset / Math.max(length, 1e-4F), f1 = 1F - f0;
        float uvv0 = 0F, uvv1 = Math.abs(yEnd - yStart) / CHAIN_SCALE;
        build4Sides(builder, f0, f1, uvv0, uvv1, normalA, normalB, vert00A, vert01A, vert10A, vert11A, vert00B, vert01B, vert10B, vert11B);
    }

    private void buildFace(ChainModel.Builder builder, Vector3f endPosition, float slack) {
        float desiredSegmentLength = 1f / CommonClass.runtimeConfig.getQuality();
        float distance = endPosition.length();
        float distanceXZ = (float) Math.sqrt(Math.fma(endPosition.x(), endPosition.x(), endPosition.z() * endPosition.z()));
        final float wrongDistanceFactor = distance / distanceXZ;
        final float chainHalfWidthA = (SIDE_A.x1() - SIDE_A.x0()) / 32F * CHAIN_SCALE;
        final float chainHalfWidthB = (SIDE_B.x1() - SIDE_B.x0()) / 32F * CHAIN_SCALE;
        Vector3f normal = new Vector3f(), rotAxis = new Vector3f();
        Vector3f vert00A = new Vector3f();
        Vector3f vert01A = new Vector3f();
        Vector3f vert11A = new Vector3f();
        Vector3f vert10A = new Vector3f();
        Vector3f vert00B = new Vector3f();
        Vector3f vert01B = new Vector3f();
        Vector3f vert11B = new Vector3f();
        Vector3f vert10B = new Vector3f();
        Quaternionf rotatorA = new Quaternionf();
        Quaternionf rotatorB = new Quaternionf();
        float inset = Math.min(KNOT_INSET, distanceXZ * 0.25F);
        float xLimit = distanceXZ - inset;
        float x = inset;
        MathHelper.Catenary catenary = MathHelper.Catenary.of(distance, endPosition.y(), slack);
        Vector3f segmentStart = new Vector3f(x, (float) catenary.y(x * wrongDistanceFactor), 0);
        Vector3f segmentEnd = new Vector3f();

        float uvv1 = 0;
        float uvv0;
        float f0, f1 = x / distanceXZ;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            float gradient = (float) catenary.slope(x * wrongDistanceFactor);
            x += estimateDeltaX(desiredSegmentLength, gradient);
            x = Math.min(xLimit, x);

            f0 = f1;
            f1 = x / distanceXZ;

            float y = (float) catenary.y(x * wrongDistanceFactor);
            segmentEnd.set(x, y, 0);

            rotAxis.set(segmentEnd.x() - segmentStart.x(), segmentEnd.y() - segmentStart.y(), segmentEnd.z() - segmentStart.z());
            rotAxis.normalize();
            rotatorA = rotatorA.fromAxisAngleDeg(rotAxis, 45);
            rotatorB = rotatorB.fromAxisAngleDeg(rotAxis, -45);

            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            Vector3f normalA = new Vector3f(), normalB = new Vector3f();
            normal.rotate(rotatorA, normalA);
            normal.rotate(rotatorB, normalB);

            normalA.normalize(chainHalfWidthA * SQRT_2);
            normalB.normalize(chainHalfWidthB * SQRT_2);

            if (segment == 0) {
                vert00A.set(segmentStart).sub(normalA);
                vert01A.set(segmentStart).add(normalA);
                vert00B.set(segmentStart).sub(normalB);
                vert01B.set(segmentStart).add(normalB);
            } else {
                vert00A.set(vert10A);
                vert01A.set(vert11A);
                vert00B.set(vert10B);
                vert01B.set(vert11B);
            }
            vert10A.set(segmentEnd).sub(normalA);
            vert11A.set(segmentEnd).add(normalA);
            vert10B.set(segmentEnd).sub(normalB);
            vert11B.set(segmentEnd).add(normalB);

            float actualSegmentLength = segmentStart.distance(segmentEnd);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / CHAIN_SCALE;

            build4Sides(builder, f0, f1, uvv0, uvv1, normalA, normalB, vert00A, vert01A, vert10A, vert11A, vert00B, vert01B, vert10B, vert11B);

            if (x >= xLimit) {
                break;
            }
            segmentStart.set(segmentEnd);
        }
    }

    private void build4Sides(ChainModel.Builder builder, float f0, float f1, float uvv0, float uvv1, Vector3f normalA, Vector3f normalB, Vector3f vert00A, Vector3f vert01A, Vector3f vert10A, Vector3f vert11A, Vector3f vert00B, Vector3f vert01B, Vector3f vert10B, Vector3f vert11B) {
        float a0 = SIDE_A.x0() / 16f;
        float a1 = SIDE_A.x1() / 16f;
        float b0 = SIDE_B.x0() / 16f;
        float b1 = SIDE_B.x1() / 16f;

        Vector3f outBMinusA = new Vector3f(normalB).sub(normalA);
        Vector3f outAMinusB = new Vector3f(outBMinusA).negate();
        Vector3f outNegSum = new Vector3f(normalA).add(normalB).negate();
        Vector3f outSum = new Vector3f(outNegSum).negate();

        addQuad(builder, f0, f1, a0, a1, uvv0, uvv1, vert00A, vert01B, vert11B, vert10A, outBMinusA);
        addQuad(builder, f0, f1, b0, b1, uvv0, uvv1, vert00A, vert00B, vert10B, vert10A, outNegSum);
        addQuad(builder, f0, f1, a1, a0, uvv0, uvv1, vert00B, vert01A, vert11A, vert10B, outAMinusB);
        addQuad(builder, f0, f1, b0, b1, uvv0, uvv1, vert01A, vert01B, vert11B, vert11A, outSum);
    }
}