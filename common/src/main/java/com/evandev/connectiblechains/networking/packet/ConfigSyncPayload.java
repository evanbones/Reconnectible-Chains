package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ConfigSyncPayload(float chainHangAmount, int maxChainRange,
                                boolean collisionsEnabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CommonClass.MODID, "config_sync"));

    public static final StreamCodec<ByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ConfigSyncPayload::chainHangAmount,
            ByteBufCodecs.INT, ConfigSyncPayload::maxChainRange,
            ByteBufCodecs.BOOL, ConfigSyncPayload::collisionsEnabled,
            ConfigSyncPayload::new
    );

    public static void handle(ConfigSyncPayload payload) {
        try {
            CommonClass.LOGGER.info("Received {} config from server", CommonClass.MODID);
            CommonClass.runtimeConfig.setChainHangAmount(payload.chainHangAmount());
            CommonClass.runtimeConfig.setMaxChainRange(payload.maxChainRange());
            CommonClass.runtimeConfig.setCollisionsEnabled(payload.collisionsEnabled());

            if (ClientInitializer.getInstance() != null) {
                ClientInitializer.getInstance().getChainKnotEntityRenderer().ifPresent(r -> r.getChainRenderer().purge());
            }
        } catch (Exception e) {
            CommonClass.LOGGER.error("Could not deserialize config: ", e);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}