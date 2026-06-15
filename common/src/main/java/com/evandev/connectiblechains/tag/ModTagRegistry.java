package com.evandev.connectiblechains.tag;

import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTagRegistry {
    public static final TagKey<Block> CHAIN_CONNECTIBLE = makeTag(Registries.BLOCK, MathHelper.identifier("chain_connectible"));
    public static final TagKey<Item> CATENARY_ITEMS = makeTag(Registries.ITEM, MathHelper.identifier("catenary_items"));
    public static final TagKey<Item> ROPES = makeTag(Registries.ITEM, MathHelper.identifier("ropes"));
    public static final TagKey<Item> BUNTING_ITEMS = makeTag(Registries.ITEM, MathHelper.identifier("bunting_items"));
    public static final TagKey<Item> HANGABLE_ITEMS = makeTag(Registries.ITEM, MathHelper.identifier("hangable_items"));
    public static final TagKey<Item> BUNTING_CHAIN_SOURCES = makeTag(Registries.ITEM, MathHelper.identifier("bunting_chain_sources"));
    public static final TagKey<Item> BANNER_CHAIN_SOURCES = makeTag(Registries.ITEM, MathHelper.identifier("banner_chain_sources"));
    public static final TagKey<Item> HANGING_CHAIN_SOURCES = makeTag(Registries.ITEM, MathHelper.identifier("hanging_chain_sources"));

    public static <T> TagKey<T> makeTag(ResourceKey<? extends net.minecraft.core.Registry<T>> registry, ResourceLocation id) {
        return TagKey.create(registry, id);
    }
}
