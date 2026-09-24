package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.evandev.connectiblechains.client.render.entity.state.ChainKnotEntityRenderState;
import com.evandev.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.HangingBlockRenderer;
import com.evandev.connectiblechains.util.MathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity, ChainKnotEntityRenderState> {
    private static final Identifier BANNER_CONNECTOR_TEXTURE =
            Identifier.fromNamespaceAndPath(CommonClass.MODID, "textures/block/banner_connector.png");
    private static final RenderType BANNER_CONNECTOR_RENDER_TYPE = RenderTypes.entityCutout(BANNER_CONNECTOR_TEXTURE);

    private static final int BANNER_PATTERN_CACHE_SIZE = 256;

    private final ChainKnotEntityModel model;
    private final ChainRenderer chainRenderer = new ChainRenderer();
    private final BannerFlagModel bannerFlagModel;
    private final SpriteGetter sprites;
    private final HangingBlockRenderer hangingBlockRenderer = new HangingBlockRenderer();

    private final Map<CompoundTag, BannerPatternLayers> bannerPatternCache =
            new LinkedHashMap<>(BANNER_PATTERN_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CompoundTag, BannerPatternLayers> eldest) {
                    return size() > BANNER_PATTERN_CACHE_SIZE;
                }
            };
    private Level lastLevel;

    public ChainKnotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel(context.bakeLayer(ClientInitializer.CHAIN_KNOT));
        this.sprites = context.getSprites();

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        this.bannerFlagModel = new BannerFlagModel(LayerDefinition.create(mesh, 64, 64).bakeRoot());

        ClientInitializer.getInstance().setChainKnotEntityRenderer(this);
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }

    public void onResourceReload() {
        bannerPatternCache.clear();
        hangingBlockRenderer.clear();
        chainRenderer.purge();
    }

    @Override
    public @NotNull ChainKnotEntityRenderState createRenderState() {
        return new ChainKnotEntityRenderState();
    }

//? if <=26.2 {
    @Override
    protected AABB getBoundingBoxForCulling(ChainKnotEntity entity) {
        AABB result = super.getBoundingBoxForCulling(entity);
//?} else {
  /*@Override
    protected AABB getBoundingBoxForCulling(ChainKnotEntity entity, float partialTick) {
        AABB result = super.getBoundingBoxForCulling(entity, partialTick);
*///?}
        for (Chainable.ChainData chainData : entity.getChainDataSet()) {
            Entity chainHolder = chainData.getResolvedHolder();
            if (chainHolder == null) {
                chainHolder = entity.getChainHolder(chainData);
            }
            if (chainHolder == null) continue;

            if (!Chainable.isValidChainDistance(entity.position(), chainHolder.position())) continue;

            result = result.minmax(chainHolder.getBoundingBox());

            double distance = entity.position().distanceTo(chainHolder.position());
            double dy = chainHolder.getY() - entity.getY();
            double sag = Math.abs(MathHelper.drip2(distance / 2.0, distance, dy, chainData.getSlack()));

            double minY = Math.min(entity.getY(), chainHolder.getY()) - sag - 1.0;
            result = result.minmax(new AABB(entity.getX(), minY, entity.getZ(), entity.getX(), minY, entity.getZ()));
        }
        return result.inflate(1.0);
    }

    @Override
    public void extractRenderState(ChainKnotEntity entity, ChainKnotEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);

        discardStateOnLevelChange(entity.level());

        state.attachedFace = entity.attachedFace;
        state.sourceItem = entity.getSourceItem();
        state.knotScaleXZ = entity.getKnotScale();
        state.knotTintColor = computeKnotTintColor(entity.level(), entity.getSourceItem(), entity.blockPosition());

        state.reset();
        Level level = entity.level();
        Vec3 entityPos = entity.getPosition(tickDelta);

        for (Chainable.ChainData chainData : new HashSet<>(entity.getChainDataSet())) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder == null) continue;

            Vec3 srcPos = entity.getChainPos(tickDelta);
            Vec3 dstPos = (chainHolder instanceof ChainKnotEntity chainKnotEntity)
                    ? chainKnotEntity.getChainPos(tickDelta)
                    : chainHolder.getRopeHoldPosition(tickDelta);

            if (!Chainable.isValidChainDistance(srcPos, dstPos)) {
                continue;
            }

            int startPackedLight = this.getPackedLightCoords(entity, tickDelta);
            int endPackedLight = this.entityRenderDispatcher.getPackedLightCoords(chainHolder, tickDelta);

            ChainKnotEntityRenderState.ChainData renderChainData = state.claim();
            renderChainData.offset = srcPos.subtract(entityPos);
            renderChainData.startPos = srcPos;
            renderChainData.endPos = dstPos;
            renderChainData.chainedEntityBlockLight = LightCoordsUtil.block(startPackedLight);
            renderChainData.chainHolderBlockLight = LightCoordsUtil.block(endPackedLight);
            renderChainData.chainedEntitySkyLight = LightCoordsUtil.sky(startPackedLight);
            renderChainData.chainHolderSkyLight = LightCoordsUtil.sky(endPackedLight);
            renderChainData.sourceItem = chainData.sourceItem;
            renderChainData.tintColor = computeChainTintColor(level, chainData.sourceItem, srcPos, dstPos);
            renderChainData.useBaked = chainHolder instanceof HangingEntity;
            renderChainData.slack = chainData.getSlack();
            renderChainData.buntings = chainData.buntings.isEmpty() ? List.of() : new ArrayList<>(chainData.buntings);
            renderChainData.banners = chainData.banners.isEmpty() ? List.of() : new ArrayList<>(chainData.banners);
            renderChainData.hangings = chainData.hangings.isEmpty() ? List.of() : new ArrayList<>(chainData.hangings);
        }

        if (CommonClass.runtimeConfig.doDebugDraw()) {
            state.nameTag = Component.literal("C: " + state.chainDataSet.size());
        }
    }

    private void discardStateOnLevelChange(Level level) {
        if (lastLevel != level) {
            lastLevel = level;
            chainRenderer.purge();
            hangingBlockRenderer.clear();
            bannerPatternCache.clear();
        }
    }

    @Override
    public void submit(ChainKnotEntityRenderState state, PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
        poseStack.pushPose();

        Direction face = state.attachedFace;
        if (face != null) {
            switch (face) {
                case DOWN -> poseStack.mulPose(new Quaternionf().rotateX((float) Math.PI));
                case NORTH -> poseStack.mulPose(new Quaternionf().rotateX((float) -Math.PI / 2f));
                case SOUTH -> poseStack.mulPose(new Quaternionf().rotateX((float) Math.PI / 2f));
                case WEST -> poseStack.mulPose(new Quaternionf().rotateZ((float) Math.PI / 2f));
                case EAST -> poseStack.mulPose(new Quaternionf().rotateZ((float) -Math.PI / 2f));
                case UP -> {
                }
            }
        }

        poseStack.translate(0, 0.5, 0);
        poseStack.scale(state.knotScaleXZ, 1, state.knotScaleXZ);

        RenderType knotType = RenderTypes.entityCutoutCull(getKnotTexture(state.sourceItem));

        collector.submitCustomGeometry(poseStack, knotType, (pose, vertexConsumer) -> {
            PoseStack tempStack = new PoseStack();
            tempStack.last().pose().set(pose.pose());
            tempStack.last().normal().set(pose.normal());
            this.model.renderToBuffer(tempStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, state.knotTintColor);
        });
        poseStack.popPose();

        List<ChainKnotEntityRenderState.ChainData> chainDataSet = state.chainDataSet;
        for (ChainKnotEntityRenderState.ChainData chainData : chainDataSet) {
            renderChainLink(poseStack, collector, chainData);
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void renderChainLink(PoseStack poseStack, SubmitNodeCollector collector, ChainKnotEntityRenderState.ChainData chainData) {
        Vec3 offset = chainData.offset;
        Vec3 startPos = chainData.startPos;
        Vec3 endPos = chainData.endPos;

        if (!Chainable.isValidChainDistance(startPos, endPos)) return;

        Item sourceItem = chainData.sourceItem;

        CatenaryRenderer renderer = getCatenaryRenderer(sourceItem);
        RenderType chainType = CommonClass.runtimeConfig.doDebugDraw() ? RenderTypes.lines()
                : renderer.isShaded() ? RenderTypes.entityCutout(getChainTexture(sourceItem))
                : RenderTypes.entityCutoutCull(getChainTexture(sourceItem));

        Vector3f chainVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));
        float angleY = -(float) Math.atan2(chainVec.z(), chainVec.x());

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        poseStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        collector.submitCustomGeometry(poseStack, chainType, (pose, vertexConsumer) -> {
            PoseStack tempStack = new PoseStack();
            tempStack.last().pose().set(pose.pose());
            tempStack.last().normal().set(pose.normal());

            if (chainData.useBaked) {
                chainRenderer.renderBaked(renderer, vertexConsumer, tempStack, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight, chainData.tintColor);
            } else {
                chainRenderer.render(renderer, vertexConsumer, tempStack, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight, chainData.tintColor);
            }

            if (CommonClass.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(tempStack, startPos, endPos, vertexConsumer);
            }
        });

        if (!chainData.banners.isEmpty()) {
            renderBannersAlongChain(poseStack, collector, chainVec, chainData);
        }

        if (!chainData.hangings.isEmpty()) {
            renderHangingsAlongChain(poseStack, collector, chainVec, chainData);
        }

        poseStack.popPose();
    }

    private void renderBannersAlongChain(PoseStack poseStack, SubmitNodeCollector collector, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        float distanceXZ = (float) Math.sqrt(chainVec.x() * chainVec.x() + chainVec.z() * chainVec.z());
        if (distanceXZ < 0.1f) return;

        float distance = chainVec.length();
        float wrongDistanceFactor = distance / distanceXZ;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        MathHelper.Catenary catenary = MathHelper.Catenary.of(distance, chainVec.y(), chainData.slack);

        for (Chainable.ChainData.BannerEntry entry : chainData.banners) {
            float t = entry.t();
            float x = t * distanceXZ;
            float y = (float) catenary.y(x * wrongDistanceFactor);

            int light = lerpLight(chainData, t);
            BannerPatternLayers patterns = bannerPatterns(mc.level, entry.data());

            final float fx = x, fy = y;
            collector.submitCustomGeometry(poseStack, BANNER_CONNECTOR_RENDER_TYPE, (pose, vertexConsumer) ->
                    renderBannerConnector(pose, vertexConsumer, light, fx, fy));

            poseStack.pushPose();
            poseStack.translate(x, y, 0);
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.PI));
            poseStack.scale(0.66f, 0.66f, 0.66f);
            poseStack.translate(0, 0.375, 0.07);
//? if <=26.2 {
            collector.submitModel(bannerFlagModel, 0.0F, poseStack, light, OverlayTexture.NO_OVERLAY, -1,
                    Sheets.BANNER_BASE, sprites, 0, null);
            BannerRenderer.submitPatterns(sprites, poseStack, collector, light, OverlayTexture.NO_OVERLAY,
                    bannerFlagModel, 0.0F, true, entry.color(), patterns, null);
//?} else {
          /*collector.submitModel(bannerFlagModel, 0.0F, poseStack, light, OverlayTexture.NO_OVERLAY, -1,
                    Sheets.BANNER_BASE, sprites, 0);
            BannerRenderer.submitPatterns(sprites, poseStack, collector, light, OverlayTexture.NO_OVERLAY,
                    bannerFlagModel, 0.0F, true, entry.color(), patterns);
*///?}
            poseStack.popPose();
        }
    }

    private void renderHangingsAlongChain(PoseStack poseStack, SubmitNodeCollector collector, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        float distanceXZ = (float) Math.sqrt(chainVec.x() * chainVec.x() + chainVec.z() * chainVec.z());
        if (distanceXZ < 0.1f) return;

        float distance = chainVec.length();
        float wrongDistanceFactor = distance / distanceXZ;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        MathHelper.Catenary curve = MathHelper.Catenary.of(distance, chainVec.y(), chainData.slack);

        for (Chainable.ChainData.HangingEntry entry : chainData.hangings) {
            float t = entry.t();
            float x = t * distanceXZ;
            float y = (float) curve.y(x * wrongDistanceFactor);

            double worldX = Mth.lerp(t, chainData.startPos.x(), chainData.endPos.x());
            double worldY = chainData.startPos.y() + y;
            double worldZ = Mth.lerp(t, chainData.startPos.z(), chainData.endPos.z());
            BlockPos pos = BlockPos.containing(worldX, worldY - 1.0, worldZ);

            hangingBlockRenderer.render(mc, entry.blockId(), pos, poseStack, collector, x, y);
        }
    }

    private int lerpLight(ChainKnotEntityRenderState.ChainData chainData, float t) {
        int blockLight = (int) Mth.lerp(t, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight);
        int skyLight = (int) Mth.lerp(t, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
        return LightCoordsUtil.pack(blockLight, skyLight);
    }

    private BannerPatternLayers bannerPatterns(Level level, CompoundTag data) {
        if (!data.contains("Pattern")) return BannerPatternLayers.EMPTY;

        BannerPatternLayers cached = bannerPatternCache.get(data);
        if (cached != null) return cached;

        BannerPatternLayers parsed = BannerPatternLayers.CODEC
                .parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), data.get("Pattern"))
                .result().orElse(BannerPatternLayers.EMPTY);
        bannerPatternCache.put(data.copy(), parsed);
        return parsed;
    }

    private void renderBannerConnector(PoseStack.Pose pose, VertexConsumer vc, int light, float x, float y) {
        float hw = 6.0f / 16.0f;
        float h = 6.0f / 16.0f;
        float u1 = 12.0f / 16.0f;
        float v1 = 6.0f / 16.0f;
        float zF = 0.005f, zB = -0.005f;

        // Front face
        vc.addVertex(pose, x - hw, y, zF).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x - hw, y - h, zF).setColor(255, 255, 255, 255).setUv(0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x + hw, y - h, zF).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x + hw, y, zF).setColor(255, 255, 255, 255).setUv(u1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);

        // Back face
        vc.addVertex(pose, x + hw, y, zB).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x + hw, y - h, zB).setColor(255, 255, 255, 255).setUv(0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x - hw, y - h, zB).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x - hw, y, zB).setColor(255, 255, 255, 255).setUv(u1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
    }

    private void drawDebugVector(PoseStack matrices, Vec3 startPos, Vec3 endPos, VertexConsumer buffer) {
        if (startPos == null) return;
        Matrix4f matrix = matrices.last().pose();
        Vec3 vec = endPos.subtract(startPos);
        Vec3 normal = vec.normalize();

        addVertex(buffer, matrix, 0, 0, 0, 0, 255, 0, 255, normal);
        addVertex(buffer, matrix, (float) vec.x, (float) vec.y, (float) vec.z, 255, 0, 0, 255, normal);
    }

    private void addVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int r, int g, int b, int a, Vec3 normal) {
        Vector4f vector = new Vector4f(x, y, z, 1.0F);
        vector.mul(matrix);
        buffer.addVertex(vector.x, vector.y, vector.z)
                .setColor(r, g, b, a)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z)
                .setLineWidth(1.0F);
    }

    private ChainTextureManager getTextureManager() {
        return ClientInitializer.getInstance().getChainTextureManager();
    }

    private Identifier getKnotTexture(Item item) {
        return getTextureManager().getKnotTexture(item);
    }

    private Identifier getChainTexture(Item item) {
        return getTextureManager().getChainTexture(item);
    }

    private CatenaryRenderer getCatenaryRenderer(Item item) {
        return getTextureManager().getCatenaryRenderer(item);
    }

    private int computeChainTintColor(Level level, Item sourceItem, Vec3 srcPos, Vec3 dstPos) {
        return getTextureManager().getTint(sourceItem)
                .map(tint -> 0xFF000000 | sampleBiomeColor(level, tint, BlockPos.containing(srcPos.lerp(dstPos, 0.5))))
                .orElse(0xFFFFFFFF);
    }

    private int computeKnotTintColor(Level level, Item sourceItem, BlockPos pos) {
        return getTextureManager().getTint(sourceItem)
                .map(tint -> 0xFF000000 | sampleBiomeColor(level, tint, pos))
                .orElse(0xFFFFFFFF);
    }

    private int sampleBiomeColor(Level level, String tintType, BlockPos pos) {
        if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) return 0xCCCCCC;
        return switch (tintType) {
            case "foliage" -> BiomeColors.getAverageFoliageColor(clientLevel, pos);
            case "grass" -> BiomeColors.getAverageGrassColor(clientLevel, pos);
            case "water" -> BiomeColors.getAverageWaterColor(clientLevel, pos);
            default -> 0xCCCCCC;
        };
    }
}
