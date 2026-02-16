package com.evandev.connectiblechains;

import com.evandev.connectiblechains.config.ModConfig;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
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
    }
}