package com.evandev.connectiblechains.compat;

import com.evandev.connectiblechains.config.ClothConfigIntegration;
import com.evandev.connectiblechains.platform.Services;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Services.PLATFORM.isModLoaded("cloth-config")) {
            return ClothConfigIntegration::createScreen;
        }
        return null;
    }
}