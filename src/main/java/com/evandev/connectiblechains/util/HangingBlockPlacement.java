package com.evandev.connectiblechains.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class HangingBlockPlacement {

    private static final double CONTACT_TOLERANCE = 1.0E-4;

    private HangingBlockPlacement() {
    }

    public static Vec3 anchor(Vec3 src, Vec3 dst, float t, float slack) {
        double distance = src.distanceTo(dst);
        if (distance < 1.0E-4) return src;

        MathHelper.Catenary curve = MathHelper.Catenary.of(distance, dst.y() - src.y(), slack);
        return new Vec3(
                Mth.lerp(t, src.x(), dst.x()),
                src.y() + curve.y(t * distance),
                Mth.lerp(t, src.z(), dst.z()));
    }

    @Nullable
    public static BlockState hangingState(Identifier blockId) {
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (block == Blocks.AIR) return null;

        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HANGING)) {
            state = state.setValue(BlockStateProperties.HANGING, true);
        }
        return state;
    }

    @Nullable
    public static AABB occupiedBox(BlockGetter level, BlockState state, Vec3 anchor) {
        BlockPos pos = BlockPos.containing(anchor.x(), anchor.y() - 1.0, anchor.z());

        VoxelShape shape;
        try {
            shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) shape = state.getShape(level, pos);
        } catch (Exception e) {
            return null;
        }
        if (shape.isEmpty()) return null;

        return shape.bounds().move(anchor.x() - 0.5, anchor.y() - 1.0, anchor.z() - 0.5);
    }

    public static boolean isObstructedByHangingBlock(Level level, BlockState state, BlockPos pos, CollisionContext context) {
        VoxelShape shape;
        try {
            shape = state.getCollisionShape(level, pos, context);
        } catch (Exception e) {
            return false;
        }
        if (shape.isEmpty()) return false;

        for (AABB box : shape.toAabbs()) {
            AABB moved = box.move(pos).deflate(CONTACT_TOLERANCE);
            if (ChainCollisionIndex.intersectsHangingBlock(level, moved)) return true;
        }
        return false;
    }

    public static boolean fitsInWorld(Level level, BlockState state, Vec3 anchor) {
        AABB box = occupiedBox(level, state, anchor);
        if (box == null) return true;

        for (VoxelShape shape : level.getBlockCollisions(null, box.deflate(CONTACT_TOLERANCE))) {
            if (!shape.isEmpty()) return false;
        }
        return true;
    }
}
