package com.evandev.connectiblechains.client.render.entity.texture;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.UVRect;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryModel;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.util.MathHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChainTextureManager extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation DEFAULT_CATENARY = MathHelper.identifier("cross");
    public static final Pair<UVRect, UVRect> DEFAULT_UV = new Pair<>(UVRect.DEFAULT_SIDE_A, UVRect.DEFAULT_SIDE_B);
    private static final String MODEL_FILE_LOCATION = "models/entity/" + CommonClass.MODID;
    private static final Gson GSON = new GsonBuilder().create();
    private static final int EXPECTED_UNIQUE_CHAIN_COUNT = 64;
    private Map<ResourceLocation, CatenaryModel> models = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);

    public ChainTextureManager() {
        super(GSON, MODEL_FILE_LOCATION);
    }

    private static @NotNull ResourceLocation defaultChainTextureId(ResourceLocation itemId) {
        return new ResourceLocation(itemId.getNamespace(), "block/%s".formatted(itemId.getPath()));
    }

    private static @NotNull ResourceLocation defaultKnotTextureId(ResourceLocation itemId) {
        return new ResourceLocation(itemId.getNamespace(), "item/%s".formatted(itemId.getPath()));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
        clearCache();
        this.models = prepared.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, o -> CatenaryModel.CODEC.codec().parse(JsonOps.INSTANCE, o.getValue()).get().orThrow()));
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

    public ResourceLocation getChainTexture(Item sourceItem) {
        ResourceLocation sourceItemId = BuiltInRegistries.ITEM.getKey(sourceItem);
        return Optional.ofNullable(models.get(sourceItemId))
                .flatMap(CatenaryModel::textures)
                .flatMap(CatenaryModel.CatenaryTextures::chainTexture)
                .orElseGet(() -> {
                    if (sourceItem instanceof BlockItem blockItem) {
                        try {
                            BlockState state = blockItem.getBlock().defaultBlockState();
                            TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon();
                            return sprite.contents().name();
                        } catch (Exception e) {
                            // Fallback to default if model loading fails
                        }
                    }
                    return defaultChainTextureId(sourceItemId);
                }).withPath(p -> {
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

    public Optional<String> getTint(Item sourceItem) {
        ResourceLocation sourceItemId = BuiltInRegistries.ITEM.getKey(sourceItem);
        return Optional.ofNullable(models.get(sourceItemId)).flatMap(CatenaryModel::tint);
    }

    public ResourceLocation getKnotTexture(Item sourceItem) {
        ResourceLocation sourceItemId = BuiltInRegistries.ITEM.getKey(sourceItem);
        return Optional.ofNullable(models.get(sourceItemId))
                .flatMap(CatenaryModel::textures)
                .flatMap(CatenaryModel.CatenaryTextures::knotTexture)
                .orElseGet(() -> {
                    try {
                        TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(new ItemStack(sourceItem)).getParticleIcon();
                        return sprite.contents().name();
                    } catch (Exception e) {
                        // Fallback to default if model loading fails
                    }
                    return defaultKnotTextureId(sourceItemId);
                }).withPath(p -> {
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