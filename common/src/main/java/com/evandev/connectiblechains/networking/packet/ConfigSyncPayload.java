package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ConfigSyncPayload(float chainHangAmount, int maxChainRange,
                                boolean collisionsEnabled) implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> TYPE = new Type<>(MathHelper.identifier("s2c_config_sync_packet_id"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> payload.write(buf),
            ConfigSyncPayload::new
    );

    public ConfigSyncPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readFloat(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(ConfigSyncPayload payload) {
        try {
            CommonClass.LOGGER.info("Received {} config from server", CommonClass.MODID);
            CommonClass.runtimeConfig.setChainHangAmount(payload.chainHangAmount);
            CommonClass.runtimeConfig.setMaxChainRange(payload.maxChainRange);
            CommonClass.runtimeConfig.setCollisionsEnabled(payload.collisionsEnabled);

            if (ClientInitializer.getInstance() != null) {
                ClientInitializer.getInstance().getChainKnotEntityRenderer().ifPresent(r -> r.getChainRenderer().purge());
            }
        } catch (Exception e) {
            CommonClass.LOGGER.error("Could not deserialize config: ", e);
        }
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(chainHangAmount);
        buf.writeInt(maxChainRange);
        buf.writeBoolean(collisionsEnabled);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}