/*
 * Copyright (C) 2024 legoatoom
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.evandev.connectiblechains.networking.packet;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.util.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;

public record ConfigSyncPayload(float chainHangAmount, int maxChainRange, boolean collisionsEnabled) implements FabricPacket {
    public static final PacketType<ConfigSyncPayload> TYPE = PacketType.create(Helper.identifier("s2c_config_sync_packet_id"), ConfigSyncPayload::new);

    public ConfigSyncPayload(PacketByteBuf buf) {
        this(buf.readFloat(), buf.readInt(), buf.readBoolean());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeFloat(chainHangAmount);
        buf.writeInt(maxChainRange);
        buf.writeBoolean(collisionsEnabled);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayerEntity ignoredClientPlayerEntity, PacketSender ignoredPacketSender) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer()) {
            return;
        }
        try {
            CommonClass.LOGGER.info("Received {} config from server", CommonClass.MODID);
            CommonClass.runtimeConfig.setChainHangAmount(this.chainHangAmount);
            CommonClass.runtimeConfig.setMaxChainRange(this.maxChainRange);
            CommonClass.runtimeConfig.setCollisionsEnabled(this.collisionsEnabled);
        } catch (Exception e) {
            CommonClass.LOGGER.error("Could not deserialize config: ", e);
        }
        ClientInitializer.getInstance().getChainKnotEntityRenderer().ifPresent(r -> r.getChainRenderer().purge());
    }
}
