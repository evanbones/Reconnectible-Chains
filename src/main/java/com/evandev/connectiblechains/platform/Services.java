package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.platform.services.INetworkHelper;
import com.evandev.connectiblechains.platform.services.IPlatformHelper;
import com.evandev.connectiblechains.platform.services.IRegistryHelper;

import java.util.ServiceLoader;

public class Services {
//? if fabric {
    public static final IPlatformHelper PLATFORM = new com.evandev.connectiblechains.platform.fabric.FabricPlatformHelper();
    public static final IRegistryHelper REGISTRY = new com.evandev.connectiblechains.platform.fabric.FabricRegistryHelper();
    public static final INetworkHelper NETWORK = new com.evandev.connectiblechains.platform.fabric.FabricNetworkHelper();
//?} else if neoforge {
  /*public static final IPlatformHelper PLATFORM = new com.evandev.connectiblechains.platform.neoforge.NeoForgePlatformHelper();
    public static final IRegistryHelper REGISTRY = new com.evandev.connectiblechains.platform.neoforge.NeoForgeRegistryHelper();
    public static final INetworkHelper NETWORK = new com.evandev.connectiblechains.platform.neoforge.NeoForgeNetworkHelper();
*///?}
}