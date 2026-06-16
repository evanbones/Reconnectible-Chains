package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record DecorationRemoveC2SPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DecorationRemoveC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CommonClass.MODID, "c2s_decoration_remove"));

    public static final DecorationRemoveC2SPacket INSTANCE = new DecorationRemoveC2SPacket();
    public static final StreamCodec<ByteBuf, DecorationRemoveC2SPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handle(Player player) {
        InteractionHand hand = player.getMainHandItem().isEmpty() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ChainRaycastHelper.tryRemoveDecoration(player, hand);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
