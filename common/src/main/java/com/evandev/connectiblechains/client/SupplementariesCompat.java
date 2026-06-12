package com.evandev.connectiblechains.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.supplementaries.client.renderers.tiles.BuntingBlockTileRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

public class SupplementariesCompat {

    public static void renderBunting(DyeColor color, PoseStack matrices, MultiBufferSource buffers,
                                     int light, BlockPos pos, long gameTime) {
        BuntingBlockTileRenderer.renderBunting(
                color, Direction.WEST, 0,
                matrices, null, buffers,
                light, OverlayTexture.NO_OVERLAY,
                pos, gameTime
        );
    }
}
