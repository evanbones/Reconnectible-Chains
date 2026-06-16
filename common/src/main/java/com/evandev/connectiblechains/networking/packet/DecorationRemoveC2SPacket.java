package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class DecorationRemoveC2SPacket {

    public DecorationRemoveC2SPacket() {
    }

    public DecorationRemoveC2SPacket(FriendlyByteBuf buf) {
    }

    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(DecorationRemoveC2SPacket packet, Player player) {
        InteractionHand hand = player.getMainHandItem().isEmpty() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ChainRaycastHelper.tryRemoveDecoration(player, hand);
    }
}
