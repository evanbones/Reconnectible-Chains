package com.evandev.connectiblechains.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ChainShapeBaker {

    private static final double HALF_WIDTH = 0.125;
    private static final double EXTENT_BELOW = 0.25;
    private static final double EXTENT_ABOVE = 0.125;
    private static final double MERGE_TOLERANCE = 0.125;

    private ChainShapeBaker() {
    }

    public static ChainShape bake(Vec3 srcPos, Vec3 dstPos, float slack, @Nullable List<AABB> extraBoxes) {
        double distance = srcPos.distanceTo(dstPos);
        if (distance < 1.0E-4) return null;

        int segments = Math.max(8, Math.min(64, (int) (distance * 1.5)));
        MathHelper.Catenary catenary = MathHelper.Catenary.of(distance, dstPos.y() - srcPos.y(), slack);

        Vec3[] samples = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double x = Mth.lerp(t, srcPos.x(), dstPos.x());
            double y = srcPos.y() + catenary.y(t * distance);
            double z = Mth.lerp(t, srcPos.z(), dstPos.z());
            samples[i] = new Vec3(x, y, z);
        }

        List<AABB> merged = new ArrayList<>();
        int start = 0;
        for (int end = 1; end <= segments; end++) {
            if (end - start >= 2 && !canMerge(samples, start, end)) {
                merged.add(enclose(samples, start, end - 1));
                start = end - 1;
            }
        }
        merged.add(enclose(samples, start, segments));

        int hangingFrom = merged.size();
        if (extraBoxes != null) merged.addAll(extraBoxes);

        AABB[] boxes = merged.toArray(new AABB[0]);
        VoxelShape[] shapes = new VoxelShape[boxes.length];
        AABB bounds = boxes[0];
        for (int i = 0; i < boxes.length; i++) {
            shapes[i] = Shapes.create(boxes[i]);
            bounds = bounds.minmax(boxes[i]);
        }

        return new ChainShape(bounds, boxes, shapes, hangingFrom);
    }

    private static boolean canMerge(Vec3[] samples, int from, int to) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (int i = from; i <= to; i++) {
            Vec3 s = samples[i];
            minX = Math.min(minX, s.x);
            minY = Math.min(minY, s.y);
            minZ = Math.min(minZ, s.z);
            maxX = Math.max(maxX, s.x);
            maxY = Math.max(maxY, s.y);
            maxZ = Math.max(maxZ, s.z);
        }

        double sizeX = maxX - minX, sizeY = maxY - minY, sizeZ = maxZ - minZ;
        double secondLargest = Math.max(Math.min(sizeX, sizeY), Math.min(Math.max(sizeX, sizeY), sizeZ));
        return secondLargest <= MERGE_TOLERANCE;
    }

    private static AABB enclose(Vec3[] samples, int from, int to) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (int i = from; i <= to; i++) {
            Vec3 s = samples[i];
            minX = Math.min(minX, s.x);
            minY = Math.min(minY, s.y);
            minZ = Math.min(minZ, s.z);
            maxX = Math.max(maxX, s.x);
            maxY = Math.max(maxY, s.y);
            maxZ = Math.max(maxZ, s.z);
        }

        return new AABB(
                minX - HALF_WIDTH, minY - EXTENT_BELOW, minZ - HALF_WIDTH,
                maxX + HALF_WIDTH, maxY + EXTENT_ABOVE, maxZ + HALF_WIDTH);
    }

    public record ChainShape(AABB bounds, AABB[] boxes, VoxelShape[] shapes, int hangingFrom) {
    }
}
