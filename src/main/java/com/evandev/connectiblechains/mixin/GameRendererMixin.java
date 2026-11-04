package com.evandev.connectiblechains.mixin;

import com.evandev.connectiblechains.client.ChainWobble;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
//? if >=26.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

//? if >=26.1 {
    @Inject(method = "bobHurt", at = @At("HEAD"))
    private void connectiblechains$chainWobble(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        ChainWobble.apply(poseStack, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
//?} else {
    /*@Inject(method = "bobHurt", at = @At("HEAD"))
    private void connectiblechains$chainWobble(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        ChainWobble.apply(poseStack, partialTick);
    }
*///?}
}
