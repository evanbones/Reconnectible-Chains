package com.evandev.connectiblechains.fabric.compat;

//? if fabric {

import com.evandev.connectiblechains.config.YaclConfigIntegration;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return YaclConfigIntegration::createScreen;
    }
}
//?}
