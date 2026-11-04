package com.evandev.connectiblechains.client;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.util.ChainCollisionIndex;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class ChainWobble {
    private static final float AMPLITUDE_DEGREES = 1.2F;
    private static final float PERIOD_TICKS = 12.0F;
    private static final float FADE_PER_TICK = 0.25F;

    private static LocalPlayer trackedPlayer;
    private static int lastTick = -1;
    private static float strength;
    private static float prevStrength;

    public static void apply(PoseStack poseStack, float partialTick) {
        float roll = roll(partialTick);
        if (roll != 0) poseStack.last().pose().rotateZ(roll * Mth.DEG_TO_RAD);
    }

    private static float roll(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || !CommonClass.runtimeConfig.isChainWobbleEnabled()) {
            strength = prevStrength = 0;
            return 0;
        }

        if (player != trackedPlayer) {
            trackedPlayer = player;
            strength = prevStrength = 0;
        }
        if (player.tickCount != lastTick) {
            lastTick = player.tickCount;
            prevStrength = strength;
            strength = Mth.clamp(strength + (isWalkingOnChain(player) ? FADE_PER_TICK : -FADE_PER_TICK), 0, 1);
        }

        float fade = Mth.lerp(partialTick, prevStrength, strength);
        if (fade == 0) return 0;

        float cycle = (player.tickCount + partialTick) / PERIOD_TICKS;
        return Mth.sin(cycle * Mth.TWO_PI) * AMPLITUDE_DEGREES * fade;
    }

    private static boolean isWalkingOnChain(LocalPlayer player) {
        if (player.getAbilities().flying || (player.getX() == player.xOld && player.getZ() == player.zOld)) return false;
        return ChainCollisionIndex.isStandingOnChain(player);
    }
}
