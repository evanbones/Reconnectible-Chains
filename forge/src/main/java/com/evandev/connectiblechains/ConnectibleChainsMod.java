package com.evandev.connectiblechains;

import com.evandev.connectiblechains.client.ClientConfigSetup;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(CommonClass.MODID)
public class ConnectibleChainsMod {
    public ConnectibleChainsMod() {

        if (FMLEnvironment.dist.isClient()) {
            ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer());
        }
    }

}