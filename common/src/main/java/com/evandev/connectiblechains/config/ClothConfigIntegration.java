package com.evandev.connectiblechains.config;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.gui.screens.Screen;

public class ClothConfigIntegration {
    public static Screen createScreen(Screen parent) {
        return AutoConfigClient.getConfigScreen(ModConfig.class, parent).get();
    }
}