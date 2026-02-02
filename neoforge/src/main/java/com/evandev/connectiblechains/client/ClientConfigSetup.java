package com.evandev.connectiblechains.client;

import com.evandev.connectiblechains.config.ClothConfigIntegration;
import com.evandev.connectiblechains.platform.Services;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientConfigSetup {
    public static void register(ModContainer container) {
        if (Services.PLATFORM.isModLoaded("cloth_config")) {
            container.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (mc, parent) -> ClothConfigIntegration.createScreen(parent)
            );
        }
    }
}