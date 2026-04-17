package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record ChainBreakC2SPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChainBreakC2SPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CommonClass.MODID, "c2s_chain_break"));

    public static final ChainBreakC2SPacket INSTANCE = new ChainBreakC2SPacket();
    public static final StreamCodec<ByteBuf, ChainBreakC2SPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handle(ChainBreakC2SPacket packet, Player player) {
        ChainRaycastHelper.tryBreakChain(player);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}