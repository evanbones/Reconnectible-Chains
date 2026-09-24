package com.evandev.connectiblechains.config;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve(CommonClass.MODID + ".json").toFile();
    private static ModConfig INSTANCE;

    private float chainHangAmount = 8.0F;
    private int maxChainRange = 32;
    private int quality = 4;
    private boolean showToolTip = true;
    private boolean showRangeWarningHud = true;
    private boolean collisionsEnabled = true;
    private boolean hangingBlockCollisions = true;
    private boolean debugDraw = false;

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                CommonClass.LOGGER.error("Failed to load {}.json", CommonClass.MODID, e);
                INSTANCE = new ModConfig();
                save();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            CommonClass.LOGGER.error("Failed to save {}.json", CommonClass.MODID, e);
        }
    }

    public float getChainHangAmount() {
        return chainHangAmount;
    }

    public void setChainHangAmount(float chainHangAmount) {
        this.chainHangAmount = chainHangAmount;
    }

    public int getMaxChainRange() {
        return maxChainRange;
    }

    public void setMaxChainRange(int maxChainRange) {
        this.maxChainRange = maxChainRange;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public boolean doDebugDraw() {
        return debugDraw;
    }

    public void setDebugDraw(boolean debugDraw) {
        this.debugDraw = debugDraw;
    }

    public boolean isCollisionsEnabled() {
        return collisionsEnabled;
    }

    public void setCollisionsEnabled(boolean collisionsEnabled) {
        this.collisionsEnabled = collisionsEnabled;
    }

    public boolean doShowToolTip() {
        return showToolTip;
    }

    public void setShowToolTip(boolean showToolTip) {
        this.showToolTip = showToolTip;
    }

    public boolean isHangingBlockCollisionsEnabled() {
        return hangingBlockCollisions;
    }

    public void setHangingBlockCollisionsEnabled(boolean hangingBlockCollisions) {
        this.hangingBlockCollisions = hangingBlockCollisions;
    }

    public void syncToClient(ServerPlayer player) {
        Services.NETWORK.sendToClient(player, new ConfigSyncPayload(chainHangAmount, maxChainRange, collisionsEnabled, hangingBlockCollisions));
    }

    public ModConfig copyFrom(ModConfig config) {
        this.chainHangAmount = config.chainHangAmount;
        this.maxChainRange = config.maxChainRange;
        this.quality = config.quality;
        this.showToolTip = config.showToolTip;
        this.showRangeWarningHud = config.showRangeWarningHud;
        this.collisionsEnabled = config.collisionsEnabled;
        this.hangingBlockCollisions = config.hangingBlockCollisions;
        this.debugDraw = config.debugDraw;
        return this;
    }

    public boolean doShowRangeWarningHud() {
        return showRangeWarningHud;
    }

    public void setShowRangeWarningHud(boolean showRangeWarningHud) {
        this.showRangeWarningHud = showRangeWarningHud;
    }
}
