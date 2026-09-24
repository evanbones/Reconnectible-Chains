package com.evandev.connectiblechains.mixin;

import com.evandev.connectiblechains.util.ChainCollisionIndex;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public class WalkNodeEvaluatorMixin {

    @Inject(method = "getPathTypeOfMob", at = @At("RETURN"), cancellable = true)
    private void connectiblechains$blockChainCells(PathfindingContext context, int x, int y, int z, Mob mob,
                                                   CallbackInfoReturnable<PathType> cir) {
        PathType original = cir.getReturnValue();
        if (mob.getPathfindingMalus(original) < 0.0F) return;

        int width = Mth.floor(mob.getBbWidth() + 1.0F);
        int height = Mth.floor(mob.getBbHeight() + 1.0F);
        AABB cells = new AABB(x, y, z, x + width, y + height, z + width);

        if (ChainCollisionIndex.intersects(mob.level(), cells)) {
            cir.setReturnValue(PathType.FENCE);
        }
    }
}
