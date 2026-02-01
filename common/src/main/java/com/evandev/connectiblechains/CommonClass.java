package com.evandev.connectiblechains;

import com.evandev.connectiblechains.config.ModConfig;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;

public class CommonClass implements ModInitializer {

    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ModConfig fileConfig;
    public static ModConfig runtimeConfig;

    @Override
    public void onInitialize() {

        ModEntityTypes.init();
        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        fileConfig = configHolder.getConfig();
        runtimeConfig = new ModConfig().copyFrom(fileConfig);

        UseBlockCallback.EVENT.register(ChainItemCallbacks::chainUseEvent);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> fileConfig.syncToClient(handler.getPlayer()));
    }
}
