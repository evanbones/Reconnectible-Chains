package com.evandev.connectiblechains.mixin;

import com.evandev.connectiblechains.util.ChainCollisionIndex;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {

//? if <=26.1 {
    /*@ModifyReturnValue(method = "collectColliders", at = @At("RETURN"))
*///?} else {
    @ModifyReturnValue(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", at = @At("RETURN"))
//?}
    private static List<VoxelShape> connectiblechains$addChainColliders(List<VoxelShape> original,
                                                                        @Local(argsOnly = true) Level level,
                                                                        @Local(argsOnly = true) AABB boundingBox) {
        List<VoxelShape> chainShapes = ChainCollisionIndex.collect(level, boundingBox);
        if (chainShapes == null) return original;

        chainShapes.addAll(original);
        return chainShapes;
    }
}
