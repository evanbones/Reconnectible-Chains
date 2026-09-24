package com.evandev.connectiblechains.config;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.util.ChainCollisionIndex;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class YaclConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.connectiblechains.title"))
                .save(() -> {
                    ModConfig.save();
                    CommonClass.runtimeConfig.copyFrom(config);
                    ChainCollisionIndex.clearAll();
                    if (ClientInitializer.getInstance() != null) {
                        ClientInitializer.getInstance().getChainKnotEntityRenderer().ifPresent(ChainKnotEntityRenderer::onResourceReload);
                    }
                });

        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.connectiblechains.category.general"));

        generalCategory.option(Option.<Float>createBuilder()
                .name(Component.translatable("config.connectiblechains.chainHangAmount"))
                .description(OptionDescription.of(
                        Component.translatable("config.connectiblechains.chainHangAmount.tooltip.0"),
                        Component.translatable("config.connectiblechains.chainHangAmount.tooltip.1"),
                        Component.translatable("config.connectiblechains.chainHangAmount.tooltip.2")))
                .binding(8.0F, config::getChainHangAmount, config::setChainHangAmount)
                .controller(FloatFieldControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Integer>createBuilder()
                .name(Component.translatable("config.connectiblechains.maxChainRange"))
                .description(OptionDescription.of(Component.translatable("config.connectiblechains.maxChainRange.tooltip")))
                .binding(32, config::getMaxChainRange, config::setMaxChainRange)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 512).step(1))
                .build());

        generalCategory.option(Option.<Integer>createBuilder()
                .name(Component.translatable("config.connectiblechains.quality"))
                .description(OptionDescription.of(Component.translatable("config.connectiblechains.quality.tooltip")))
                .binding(4, config::getQuality, config::setQuality)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 8).step(1))
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.connectiblechains.showToolTip"))
                .description(OptionDescription.of(Component.translatable("config.connectiblechains.showToolTip.tooltip")))
                .binding(true, config::doShowToolTip, config::setShowToolTip)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.connectiblechains.showRangeWarningHud"))
                .description(OptionDescription.of(Component.translatable("config.connectiblechains.showRangeWarningHud.tooltip")))
                .binding(true, config::doShowRangeWarningHud, config::setShowRangeWarningHud)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.connectiblechains.collisionsEnabled"))
                .description(OptionDescription.of(Component.translatable("config.connectiblechains.collisionsEnabled.tooltip")))
                .binding(true, config::isCollisionsEnabled, config::setCollisionsEnabled)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.connectiblechains.hangingBlockCollisions"))
                .description(OptionDescription.of(Component.translatable("config.connectiblechains.hangingBlockCollisions.tooltip")))
                .binding(true, config::isHangingBlockCollisionsEnabled, config::setHangingBlockCollisionsEnabled)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.connectiblechains.debugDraw"))
                .description(OptionDescription.of(Component.translatable("config.connectiblechains.debugDraw.tooltip")))
                .binding(Services.PLATFORM.isDevelopmentEnvironment(), config::doDebugDraw, config::setDebugDraw)
                .controller(TickBoxControllerBuilder::create)
                .build());

        return builder
                .category(generalCategory.build())
                .build()
                .generateScreen(parent);
    }
}
