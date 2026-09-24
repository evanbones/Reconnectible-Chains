package com.evandev.connectiblechains.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.platform.Services;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class ModEntityTypes {

    public static final Supplier<EntityType<ChainKnotEntity>> CHAIN_KNOT = Services.REGISTRY.registerEntity("chain_knot",
            () -> EntityType.Builder.<ChainKnotEntity>of(ChainKnotEntity::new, MobCategory.MISC)
                    .clientTrackingRange(20)
                    .updateInterval(Integer.MAX_VALUE)
                    .sized(0.375f, 0.5F)
                    .fireImmune());

    public static void init() {
        CommonClass.LOGGER.info("Initialized entity types.");
    }
}