package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.MathHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record GlowLightsSyncS2CPacket(int entityId, int holderId,
                                      Optional<Identifier> glowLights) implements CustomPacketPayload {

    public static final Type<GlowLightsSyncS2CPacket> TYPE = new Type<>(MathHelper.identifier("s2c_glow_lights_sync"));

    public static final StreamCodec<ByteBuf, GlowLightsSyncS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GlowLightsSyncS2CPacket::entityId,
            ByteBufCodecs.INT, GlowLightsSyncS2CPacket::holderId,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), GlowLightsSyncS2CPacket::glowLights,
            GlowLightsSyncS2CPacket::new
    );

    public static void handle(GlowLightsSyncS2CPacket packet, Player player) {
        if (!(player.level().getEntity(packet.entityId()) instanceof Chainable chainable)) return;

        Chainable.ChainData data = chainable.getChainData(packet.holderId());
        if (data != null) {
            data.glowLights = packet.glowLights().orElse(null);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
