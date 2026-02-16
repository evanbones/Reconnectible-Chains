package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ConfigSyncPayload(float chainHangAmount, int maxChainRange, boolean collisionsEnabled) {
    public static final ResourceLocation TYPE = MathHelper.identifier("s2c_config_sync_packet_id");

    public ConfigSyncPayload(FriendlyByteBuf buf) {
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

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(chainHangAmount);
        buf.writeInt(maxChainRange);
        buf.writeBoolean(collisionsEnabled);
    }
}