package com.evandev.connectiblechains.platform.neoforge;

//? if neoforge {
/*import com.evandev.connectiblechains.platform.services.IPlatformHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        //? if >=26.1 {
        return !FMLLoader.getCurrent().isProduction();
        //?} else {
        /^return !FMLLoader.isProduction();
        ^///?}
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isPhysicalClient() {
        //? if >=26.1 {
        return FMLLoader.getCurrent().getDist() == Dist.CLIENT;
        //?} else {
        /^return FMLLoader.getDist() == Dist.CLIENT;
        ^///?}
    }
}
*///?}
