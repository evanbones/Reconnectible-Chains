package com.evandev.connectiblechains.mixin.compat.lithium;

import com.evandev.connectiblechains.util.ChainCollisionIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions", remap = false)
public class LithiumEntityCollisionsMixin {

    @Inject(method = "appendEntityCollisions", at = @At("RETURN"))
    private static void connectiblechains$appendChainCollisions(List<VoxelShape> entityCollisions, Level world,
                                                                Entity entity, AABB box, CallbackInfo ci) {
        List<VoxelShape> chainShapes = ChainCollisionIndex.collect(world, box);
        if (chainShapes != null) entityCollisions.addAll(chainShapes);
    }
}
