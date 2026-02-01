package com.evandev.connectiblechains.tag;

import com.evandev.connectiblechains.util.Helper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTagRegistry {
    public static final TagKey<Block> CHAIN_CONNECTIBLE = makeTag(Registries.BLOCK, Helper.identifier("chain_connectible"));
    public static final TagKey<Item> CATENARY_ITEMS = makeTag(Registries.ITEM, Helper.identifier("catenary_items"));
    public static final TagKey<Item> ROPES = makeTag(Registries.ITEM, Helper.identifier("ropes"));

    public static <T> TagKey<T> makeTag(ResourceKey<? extends net.minecraft.core.Registry<T>> registry, ResourceLocation id) {
        return TagKey.create(registry, id);
    }
}