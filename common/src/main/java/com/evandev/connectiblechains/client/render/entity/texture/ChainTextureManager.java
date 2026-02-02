package com.evandev.connectiblechains.client.render.entity.texture;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.UVRect;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryModel;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.util.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChainTextureManager extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation DEFAULT_CATENARY = Helper.identifier("cross");
    public static final Pair<UVRect, UVRect> DEFAULT_UV = new Pair<>(UVRect.DEFAULT_SIDE_A, UVRect.DEFAULT_SIDE_B);
    private static final String MODEL_FILE_LOCATION = "models/entity/" + CommonClass.MODID;
    private static final Gson GSON = new GsonBuilder().create();
    private static final int EXPECTED_UNIQUE_CHAIN_COUNT = 64;
    private Map<ResourceLocation, CatenaryModel> models = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);

    public ChainTextureManager() {
        super(GSON, MODEL_FILE_LOCATION);
    }

    private static @NotNull ResourceLocation defaultChainTextureId(ResourceLocation itemId) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "block/%s".formatted(itemId.getPath()));
    }

    private static @NotNull ResourceLocation defaultKnotTextureId(ResourceLocation itemId) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "item/%s".formatted(itemId.getPath()));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
        clearCache();
        this.models = prepared.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, o -> CatenaryModel.CODEC.codec().parse(JsonOps.INSTANCE, o.getValue()).getOrThrow()));
    }

    public void clearCache() {
        ClientInitializer.getInstance().getChainKnotEntityRenderer().ifPresent(it -> it.getChainRenderer().purge());
    }

    public CatenaryRenderer getCatenaryRenderer(ResourceLocation sourceItemId) {
        Optional<CatenaryModel> catenaryModel = Optional.ofNullable(models.get(sourceItemId));
        ResourceLocation catenaryId = catenaryModel.flatMap(CatenaryModel::catenaryRendererId).orElse(DEFAULT_CATENARY);
        Pair<UVRect, UVRect> uvMappings = catenaryModel.flatMap(CatenaryModel::uvRects).orElse(DEFAULT_UV);
        return CatenaryRenderer.getRenderer(catenaryId, uvMappings);
    }

    public ResourceLocation getChainTexture(ResourceLocation sourceItemId) {
        return Optional.ofNullable(models.get(sourceItemId)).flatMap(CatenaryModel::textures).flatMap(CatenaryModel.CatenaryTextures::chainTexture).orElse(defaultChainTextureId(sourceItemId)).withPath(p -> "textures/" + p + ".png");
    }

    public ResourceLocation getKnotTexture(ResourceLocation sourceItemId) {
        return Optional.ofNullable(models.get(sourceItemId)).flatMap(CatenaryModel::textures).flatMap(CatenaryModel.CatenaryTextures::knotTexture).orElse(defaultKnotTextureId(sourceItemId)).withPath(p -> "textures/" + p + ".png");
    }
}