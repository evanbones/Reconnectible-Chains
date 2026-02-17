package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ChainBreakC2SPacket {

    public ChainBreakC2SPacket() {
    }

    public ChainBreakC2SPacket(FriendlyByteBuf buf) {
    }

    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(ChainBreakC2SPacket packet, Player player) {
        ChainRaycastHelper.tryBreakChain(player);
    }
}