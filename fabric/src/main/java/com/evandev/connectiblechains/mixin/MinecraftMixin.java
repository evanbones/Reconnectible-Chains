package com.evandev.connectiblechains.mixin;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
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
                ClientPlayNetworking.send(new ResourceLocation(CommonClass.MODID, "c2s_chain_break"), PacketByteBufs.empty());
                this.player.swing(InteractionHand.MAIN_HAND);
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        if (this.player == null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            if (ChainRaycastHelper.tryRemoveDecoration(this.player, hand)) {
                ClientPlayNetworking.send(new ResourceLocation(CommonClass.MODID, "c2s_decoration_remove"), PacketByteBufs.empty());
                this.player.swing(hand);
                ci.cancel();
                return;
            }
        }
    }
}