package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public class HangingLightHelper {

    @Nullable
    public static BlockPos computeLightPos(Entity src, Entity dst, float t, float slack) {
        if (!(src instanceof ChainKnotEntity srcKnot)) return null;
        if (!(dst instanceof ChainKnotEntity dstKnot)) return null;
        Vec3 srcPos = srcKnot.getChainPos(1.0f);
        Vec3 dstPos = dstKnot.getChainPos(1.0f);
        double dist3D = srcPos.distanceTo(dstPos);
        if (dist3D < 0.01) return null;
        double worldX = Mth.lerp(t, srcPos.x(), dstPos.x());
        double worldY = srcPos.y() + MathHelper.drip2(t * dist3D, dist3D, dstPos.y() - srcPos.y(), slack);
        double worldZ = Mth.lerp(t, srcPos.z(), dstPos.z());
        return BlockPos.containing(worldX, worldY - 1.0, worldZ);
    }

    public static void place(ServerLevel level, BlockPos pos, int emission) {
        if (emission <= 0) return;
        int clamped = Mth.clamp(emission, 1, 15);
        BlockState target = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, clamped);
        BlockState existing = level.getBlockState(pos);
        if (!existing.equals(target) && (existing.isAir() || existing.getBlock() == Blocks.LIGHT)) {
            level.setBlock(pos, target, 3);
        }
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() == Blocks.LIGHT) {
            level.removeBlock(pos, false);
        }
    }

    public static void placeAllForChain(ServerLevel level, Entity src, Entity dst, Chainable.ChainData chainData) {
        if (chainData.hangings.isEmpty()) return;
        float slack = chainData.getSlack();
        for (Chainable.ChainData.HangingEntry entry : chainData.hangings) {
            Block block = BuiltInRegistries.BLOCK.get(entry.blockId());
            if (block == Blocks.AIR) continue;
            BlockState blockState = block.defaultBlockState();
            if (blockState.hasProperty(BlockStateProperties.HANGING)) {
                blockState = blockState.setValue(BlockStateProperties.HANGING, true);
            }
            int emission = blockState.getLightEmission();
            BlockPos pos = computeLightPos(src, dst, entry.t(), slack);
            if (pos != null) place(level, pos, emission);
        }
    }

    public static void removeAllForChain(ServerLevel level, Entity src, Entity dst, Chainable.ChainData chainData) {
        if (chainData.hangings.isEmpty()) return;
        float slack = chainData.getSlack();
        for (Chainable.ChainData.HangingEntry entry : chainData.hangings) {
            BlockPos pos = computeLightPos(src, dst, entry.t(), slack);
            if (pos != null) remove(level, pos);
        }
    }
}
