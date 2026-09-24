package com.evandev.connectiblechains.mixin;

//? if fabric {

import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.networking.packet.DecorationRemoveC2SPacket;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player != null) {
            if (ChainRaycastHelper.tryBreakChain(this.player)) {
                ClientPlayNetworking.send(ChainBreakC2SPacket.INSTANCE);
//? if <=26.2 {
                this.player.swing(InteractionHand.MAIN_HAND);
//?} else {
              /*this.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
*///?}
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        if (this.player == null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            if (ChainRaycastHelper.tryRemoveDecoration(this.player, hand)) {
                ClientPlayNetworking.send(DecorationRemoveC2SPacket.INSTANCE);
//? if <=26.2 {
                this.player.swing(hand);
//?} else {
              /*this.player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
*///?}
                ci.cancel();
                return;
            }
        }
    }
}
//?}
