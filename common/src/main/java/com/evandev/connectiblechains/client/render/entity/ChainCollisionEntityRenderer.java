package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.entity.ChainCollisionEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ChainCollisionEntityRenderer extends EntityRenderer<ChainCollisionEntity> {

    public ChainCollisionEntityRenderer(EntityRendererProvider.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ChainCollisionEntity entity) {
        return null;
    }
}