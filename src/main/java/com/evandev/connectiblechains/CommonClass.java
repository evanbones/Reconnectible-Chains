package com.evandev.connectiblechains;

import com.evandev.connectiblechains.config.ModConfig;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CommonClass {

    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ModConfig fileConfig;
    public static ModConfig runtimeConfig;

    public static void init() {
        ModEntityTypes.init();

        ModConfig.load();
        fileConfig = ModConfig.get();
        runtimeConfig = new ModConfig().copyFrom(fileConfig);
    }
}