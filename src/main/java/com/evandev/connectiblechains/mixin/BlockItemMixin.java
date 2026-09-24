package com.evandev.connectiblechains.mixin;

import com.evandev.connectiblechains.util.HangingBlockPlacement;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @ModifyReturnValue(method = "canPlace", at = @At("RETURN"))
    private boolean connectiblechains$rejectHangingBlockOverlap(boolean original, BlockPlaceContext context, BlockState stateForPlacement) {
        if (!original) return false;

        Player player = context.getPlayer();
        CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        return !HangingBlockPlacement.isObstructedByHangingBlock(
                context.getLevel(), stateForPlacement, context.getClickedPos(), collisionContext);
    }
}
