package com.evandev.connectiblechains.platform;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.platform.services.IRegistryHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class NeoForgeRegistryHelper implements IRegistryHelper {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, CommonClass.MODID);

    @Override
    public <T extends Entity> Supplier<EntityType<T>> registerEntity(String name, Supplier<EntityType.Builder<T>> builderSupplier) {
        Identifier id = Identifier.fromNamespaceAndPath(CommonClass.MODID, name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);

        return ENTITIES.register(name,
                () -> builderSupplier.get().build(key));
    }
}