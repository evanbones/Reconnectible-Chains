package com.evandev.connectiblechains.client.render.entity.texture;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.UVRect;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryModel;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.util.MathHelper;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ChainTextureManager extends SimplePreparableReloadListener<Map<Identifier, CatenaryModel>> {
    public static final Identifier DEFAULT_CATENARY = MathHelper.identifier("cross");
    public static final Pair<UVRect, UVRect> DEFAULT_UV = new Pair<>(UVRect.DEFAULT_SIDE_A, UVRect.DEFAULT_SIDE_B);
    private static final String MODEL_FILE_LOCATION = "models/entity/" + CommonClass.MODID;
    private static final int EXPECTED_UNIQUE_CHAIN_COUNT = 64;
    private Map<Identifier, CatenaryModel> models = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);

    private static @NotNull Identifier defaultChainTextureId(Identifier itemId) {
        String path = itemId.getPath();
        if (path.startsWith("waxed_")) {
            path = path.substring(6);
        }
        return Identifier.fromNamespaceAndPath(itemId.getNamespace(), "block/%s".formatted(path));
    }

    private static @NotNull Identifier defaultKnotTextureId(Identifier itemId) {
        String path = itemId.getPath();
        if (path.startsWith("waxed_")) {
            path = path.substring(6);
        }
        return Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/%s".formatted(path));
    }

    @Override
    protected Map<Identifier, CatenaryModel> prepare(@NonNull ResourceManager resourceManager, @NonNull ProfilerFiller profiler) {
        Map<Identifier, CatenaryModel> map = new HashMap<>();
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(MODEL_FILE_LOCATION);

        for (Map.Entry<Identifier, Resource> entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            Identifier id = fileToIdConverter.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(reader);
                CatenaryModel model = CatenaryModel.CODEC.codec().parse(JsonOps.INSTANCE, jsonElement).getOrThrow();
                map.put(id, model);
            } catch (Exception e) {
                CommonClass.LOGGER.error("Couldn't parse chain model {}", id, e);
            }
        }
        return map;
    }

    @Override
    protected void apply(Map<Identifier, CatenaryModel> prepared, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
        clearCache();
        this.models = new Object2ObjectOpenHashMap<>(prepared);
    }

    public void clearCache() {
        ClientInitializer.getInstance().getChainKnotEntityRenderer().ifPresent(it -> it.getChainRenderer().purge());
    }

    public CatenaryRenderer getCatenaryRenderer(Identifier sourceItemId) {
        Optional<CatenaryModel> catenaryModel = Optional.ofNullable(models.get(sourceItemId));
        Identifier catenaryId = catenaryModel.flatMap(CatenaryModel::catenaryRendererId).orElse(DEFAULT_CATENARY);
        Pair<UVRect, UVRect> uvMappings = catenaryModel.flatMap(CatenaryModel::uvRects).orElse(DEFAULT_UV);
        return CatenaryRenderer.getRenderer(catenaryId, uvMappings);
    }

    public Identifier getChainTexture(Item sourceItem) {
        Identifier sourceItemId = BuiltInRegistries.ITEM.getKey(sourceItem);
        return Optional.ofNullable(models.get(sourceItemId))
                .flatMap(CatenaryModel::textures)
                .flatMap(CatenaryModel.CatenaryTextures::chainTexture)
                .orElseGet(() -> defaultChainTextureId(sourceItemId)).withPath(p -> {
                    String path = p;
                    if (!path.startsWith("textures/")) {
                        path = "textures/" + path;
                    }
                    if (!path.endsWith(".png")) {
                        path = path + ".png";
                    }
                    return path;
                });
    }

    public Identifier getKnotTexture(Item sourceItem) {
        Identifier sourceItemId = BuiltInRegistries.ITEM.getKey(sourceItem);
        return Optional.ofNullable(models.get(sourceItemId))
                .flatMap(CatenaryModel::textures)
                .flatMap(CatenaryModel.CatenaryTextures::knotTexture)
                .orElseGet(() -> defaultKnotTextureId(sourceItemId)).withPath(p -> {
                    String path = p;
                    if (!path.startsWith("textures/")) {
                        path = "textures/" + path;
                    }
                    if (!path.endsWith(".png")) {
                        path = path + ".png";
                    }
                    return path;
                });
    }
}