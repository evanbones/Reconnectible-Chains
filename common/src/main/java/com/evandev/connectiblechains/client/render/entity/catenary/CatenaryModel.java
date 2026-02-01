package com.evandev.connectiblechains.client.render.entity.catenary;

import com.evandev.connectiblechains.client.render.entity.UVRect;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record CatenaryModel(Optional<CatenaryTextures> textures, Optional<ResourceLocation> catenaryRendererId,
                            Optional<Pair<UVRect, UVRect>> uvRects) {
    public static final MapCodec<CatenaryModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CatenaryTextures.CODEC.codec().optionalFieldOf("textures").forGetter(CatenaryModel::textures),
            ResourceLocation.CODEC.optionalFieldOf("model").forGetter(CatenaryModel::catenaryRendererId),
            UVRect.CODEC.optionalFieldOf("uv").forGetter(CatenaryModel::uvRects)
    ).apply(instance, CatenaryModel::new));

    public record CatenaryTextures(Optional<ResourceLocation> chainTexture, Optional<ResourceLocation> knotTexture) {
        public static final MapCodec<CatenaryTextures> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("chain").forGetter(CatenaryTextures::chainTexture),
                ResourceLocation.CODEC.optionalFieldOf("knot").forGetter(CatenaryTextures::knotTexture)
        ).apply(instance, CatenaryTextures::new));
    }
}