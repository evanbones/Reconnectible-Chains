package com.evandev.connectiblechains;

import com.evandev.connectiblechains.config.ModConfig;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.networking.packet.BannerSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.networking.packet.HangingSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;

public class CommonClass {

    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ModConfig fileConfig;
    public static ModConfig runtimeConfig;

    public static void init() {
        ModEntityTypes.init();

        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        fileConfig = configHolder.getConfig();
        runtimeConfig = new ModConfig().copyFrom(fileConfig);

        configHolder.registerSaveListener((holder, config) -> {
            runtimeConfig.copyFrom(config);
            return InteractionResult.PASS;
        });

        Services.NETWORK.registerClientReceiver(
                ChainAttachS2CPacket.class,
                ChainAttachS2CPacket.TYPE,
                ChainAttachS2CPacket::new
        );

        Services.NETWORK.registerClientReceiver(
                ConfigSyncPayload.class,
                new ResourceLocation(MODID, "config_sync"),
                ConfigSyncPayload::new
        );

        Services.NETWORK.registerClientReceiver(
                ChainSlackSyncS2CPacket.class,
                ChainSlackSyncS2CPacket.TYPE,
                ChainSlackSyncS2CPacket::new
        );

        Services.NETWORK.registerClientReceiver(
                BuntingSyncS2CPacket.class,
                BuntingSyncS2CPacket.TYPE,
                BuntingSyncS2CPacket::new
        );

        Services.NETWORK.registerClientReceiver(
                BannerSyncS2CPacket.class,
                BannerSyncS2CPacket.TYPE,
                BannerSyncS2CPacket::new
        );

        Services.NETWORK.registerClientReceiver(
                HangingSyncS2CPacket.class,
                HangingSyncS2CPacket.TYPE,
                HangingSyncS2CPacket::new
        );
    }
}