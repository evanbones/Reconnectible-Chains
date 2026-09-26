package com.evandev.connectiblechains.util;

//? if <26.1 {
/*import com.evandev.connectiblechains.compat.sable.SableHelper;
*///?}
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
//? if <26.1 {
/*import dev.ryanhcode.sable.companion.SubLevelAccess;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class HangingLightHelper {

    @Nullable
    public static BlockPos computeLightPos(Entity src, Entity dst, float t, float slack) {
        if (!(src instanceof ChainKnotEntity srcKnot)) return null;
        if (!(dst instanceof ChainKnotEntity dstKnot)) return null;
        Vec3 srcPos = srcKnot.getChainPos(1.0f);
        Vec3 dstPos = dstKnot.getChainPos(1.0f);
//? if <26.1 {
        /*SubLevelAccess srcSubLevel = SableHelper.getContaining(src.level(), srcPos);
        SubLevelAccess dstSubLevel = SableHelper.getContaining(dst.level(), dstPos);
        if (srcSubLevel != dstSubLevel) {
            srcPos = SableHelper.projectOutOfSubLevel(src.level(), srcPos);
            dstPos = SableHelper.projectOutOfSubLevel(dst.level(), dstPos);
        }
*///?}
        double dist = srcPos.distanceTo(dstPos);
        if (dist < 0.01 || dist > 64.0) return null;

        Vec3 anchor = HangingBlockPlacement.anchor(srcPos, dstPos, t, slack);
        return BlockPos.containing(anchor.x(), anchor.y() - 1.0, anchor.z());
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
            Block block = BuiltInRegistries.BLOCK.getValue(entry.blockId());
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
