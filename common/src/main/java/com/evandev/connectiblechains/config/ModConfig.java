package com.evandev.connectiblechains.config;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.platform.Services;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.server.level.ServerPlayer;

@Config(name = CommonClass.MODID)
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip(count = 3)
    private float chainHangAmount = 8.0F;
    @ConfigEntry.BoundedDiscrete(max = 512)
    @ConfigEntry.Gui.Tooltip()
    private int maxChainRange = 32;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
    @ConfigEntry.Gui.Tooltip()
    private int quality = 4;

    @ConfigEntry.Gui.Tooltip()
    private boolean showToolTip = true;

    @ConfigEntry.Gui.Tooltip()
    private boolean showRangeWarningHud = true;

    @ConfigEntry.Gui.Tooltip()
    private boolean collisionsEnabled = false;

    @ConfigEntry.Gui.Tooltip()
    private boolean debugDraw = Services.PLATFORM.isDevelopmentEnvironment();

    public float getChainHangAmount() {
        return chainHangAmount;
    }

    @SuppressWarnings("unused")
    public void setChainHangAmount(float chainHangAmount) {
        this.chainHangAmount = chainHangAmount;
    }

    public int getMaxChainRange() {
        return maxChainRange;
    }

    @SuppressWarnings("unused")
    public void setMaxChainRange(int maxChainRange) {
        this.maxChainRange = maxChainRange;
    }

    public int getQuality() {
        return quality;
    }

    @SuppressWarnings("unused")
    public void setQuality(int quality) {
        this.quality = quality;
    }

    public boolean doDebugDraw() {
        return debugDraw;
    }

    public boolean isCollisionsEnabled() {
        return collisionsEnabled;
    }

    public void setCollisionsEnabled(boolean collisionsEnabled) {
        this.collisionsEnabled = collisionsEnabled;
    }

    public void syncToClient(ServerPlayer player) {
        Services.NETWORK.sendToClient(player, new ConfigSyncPayload(chainHangAmount, maxChainRange, collisionsEnabled));
    }

    public ModConfig copyFrom(ModConfig config) {
        this.chainHangAmount = config.chainHangAmount;
        this.maxChainRange = config.maxChainRange;
        this.quality = config.quality;
        this.showToolTip = config.showToolTip;
        this.showRangeWarningHud = config.showRangeWarningHud;
        this.collisionsEnabled = config.collisionsEnabled;
        this.debugDraw = config.debugDraw;
        return this;
    }

    public boolean doShowToolTip() {
        return showToolTip;
    }

    public boolean doShowRangeWarningHud() {
        return showRangeWarningHud;
    }
}
