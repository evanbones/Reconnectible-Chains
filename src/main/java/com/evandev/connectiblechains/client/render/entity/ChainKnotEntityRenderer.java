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
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

//? if >=26.1 {
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.util.LightCoordsUtil;
//?} else {
/*import com.evandev.connectiblechains.client.SupplementariesCompat;
import com.evandev.connectiblechains.compat.sable.SableHelper;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
*///?}

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//? if >=26.1 {
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity, ChainKnotEntityRenderState> {
//?} else {
/*public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
*///?}
    private static final Identifier BANNER_CONNECTOR_TEXTURE =
            Identifier.fromNamespaceAndPath(CommonClass.MODID, "textures/block/banner_connector.png");
    private static final int BANNER_PATTERN_CACHE_SIZE = 256;
    private static final int NO_TINT = 0xFFFFFFFF;
    private static final int DEFAULT_BIOME_COLOR = 0xCCCCCC;
//? if >=26.1 {
    private static final RenderType BANNER_CONNECTOR_RENDER_TYPE = RenderTypes.entityCutout(BANNER_CONNECTOR_TEXTURE);
//?} else {
    /*private static final RenderType BANNER_CONNECTOR_RENDER_TYPE = RenderType.entityCutoutNoCull(BANNER_CONNECTOR_TEXTURE);
    *///?}

    private final ChainRenderer chainRenderer = new ChainRenderer();
    private final HangingBlockRenderer hangingBlockRenderer = new HangingBlockRenderer();
    private final Map<Item, RenderType> chainRenderTypes = new Reference2ObjectOpenHashMap<>();
    private final Map<Item, RenderType> knotRenderTypes = new Reference2ObjectOpenHashMap<>();
    private final Map<CompoundTag, BannerPatternLayers> bannerPatternCache =
            new LinkedHashMap<>(BANNER_PATTERN_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CompoundTag, BannerPatternLayers> eldest) {
                    return size() > BANNER_PATTERN_CACHE_SIZE;
                }
            };
    private Level lastLevel;

//? if >=26.1 {
    private final ChainKnotEntityModel model;
    private final BannerFlagModel bannerFlagModel;
    private final SpriteGetter sprites;

    public ChainKnotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel(context.bakeLayer(ClientInitializer.CHAIN_KNOT));
        this.sprites = context.getSprites();
        this.bannerFlagModel = createBannerFlagModel();

        ClientInitializer.getInstance().setChainKnotEntityRenderer(this);
    }

    private static BannerFlagModel createBannerFlagModel() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        return new BannerFlagModel(LayerDefinition.create(mesh, 64, 64).bakeRoot());
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
        Chainable.ChainData[] chains = entity.getChainDataArray();
        if (chains.length == 0) return result;

        for (Chainable.ChainData chainData : chains) {
            Entity chainHolder = chainData.getResolvedHolder();
            if (chainHolder == null) chainHolder = entity.getChainHolder(chainData);
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
        fillRenderState(entity, state, tickDelta);

        if (CommonClass.runtimeConfig.doDebugDraw()) {
            state.nameTag = Component.literal("C: " + state.chainDataSet.size());
        }
    }

    @Override
    public void submit(ChainKnotEntityRenderState state, PoseStack poseStack, @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState cameraState) {
        poseStack.pushPose();
        transformToKnot(poseStack, state);
        collector.submitCustomGeometry(poseStack, getKnotRenderType(state.sourceItem), (pose, vertexConsumer) -> {
            PoseStack tempStack = new PoseStack();
            tempStack.last().pose().set(pose.pose());
            tempStack.last().normal().set(pose.normal());
            this.model.renderToBuffer(tempStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, state.knotTintColor);
        });
        poseStack.popPose();

        for (ChainKnotEntityRenderState.ChainData chainData : state.chainDataSet) {
            renderChainLink(poseStack, collector, chainData);
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void renderChainLink(PoseStack poseStack, SubmitNodeCollector collector, ChainKnotEntityRenderState.ChainData chainData) {
        boolean debugDraw = CommonClass.runtimeConfig.doDebugDraw();
        RenderType chainType = debugDraw ? RenderTypes.lines() : getChainRenderType(chainData.sourceItem);
        Vector3f chainVec = chainData.endPos.subtract(chainData.startPos).toVector3f();

        poseStack.pushPose();
        poseStack.translate(chainData.offset.x, chainData.offset.y, chainData.offset.z);

        collector.submitCustomGeometry(poseStack, chainType, (pose, vertexConsumer) -> {
            renderChain(vertexConsumer, pose, chainVec, chainData);
            if (debugDraw) {
                drawDebugVector(pose.pose(), chainData.startPos, chainData.endPos, vertexConsumer);
            }
        });

        poseStack.mulPose(new Quaternionf().rotationY(yawOf(chainVec)));
        renderBannersAlongChain(poseStack, collector, chainVec, chainData);
        renderHangingsAlongChain(poseStack, collector, chainVec, chainData);
        poseStack.popPose();
    }

    private void renderBannersAlongChain(PoseStack poseStack, SubmitNodeCollector collector, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        if (chainData.banners.isEmpty()) return;
        ChainCurve curve = ChainCurve.of(chainVec, chainData.slack);
        Level level = Minecraft.getInstance().level;
        if (curve == null || level == null) return;

        for (Chainable.ChainData.BannerEntry entry : chainData.banners) {
            float x = curve.x(entry.t());
            float y = curve.y(x);
            int light = lerpLight(chainData, entry.t());
            BannerPatternLayers patterns = bannerPatterns(level, entry.data());

            collector.submitCustomGeometry(poseStack, BANNER_CONNECTOR_RENDER_TYPE, (pose, vertexConsumer) ->
                    renderBannerConnector(pose, vertexConsumer, light, x, y));

            poseStack.pushPose();
            transformToBanner(poseStack, x, y);
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
//?} else {
    /*private static final Item[] BUNTING_ITEMS = new Item[DyeColor.values().length];

    private final ChainKnotEntityModel<ChainKnotEntity> model;
    private final ModelPart bannerFlag;
    private final ChainKnotEntityRenderState reusableState = new ChainKnotEntityRenderState();

    public ChainKnotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel<>(context.bakeLayer(ClientInitializer.CHAIN_KNOT));
        this.bannerFlag = context.bakeLayer(ModelLayers.BANNER).getChild("flag");

        ClientInitializer.getInstance().setChainKnotEntityRenderer(this);
    }

    private static Item buntingItem(DyeColor color) {
        Item cached = BUNTING_ITEMS[color.getId()];
        if (cached == null) {
            cached = BuiltInRegistries.ITEM.getValue(
                    Identifier.fromNamespaceAndPath("supplementaries", "bunting_" + color.getName()));
            BUNTING_ITEMS[color.getId()] = cached;
        }
        return cached;
    }

    @Override
    public Identifier getTextureLocation(@NotNull ChainKnotEntity entity) {
        return null;
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, Frustum camera, double camX, double camY, double camZ) {
        double distanceSqr = SableHelper.distanceToCameraSqr(entity, this.entityRenderDispatcher);
        if (!entity.shouldRenderAtSqrDistance(distanceSqr)) return false;
        if (entity.noCulling) return true;

        AABB aabb = entity.getBoundingBoxForCulling().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0.0) {
            aabb = AABB.ofSize(entity.position(), 4.0, 4.0, 4.0);
        }

        SubLevelAccess subLevel = SableHelper.getSubLevel(entity.level(), entity, null);
        if (subLevel != null) {
            aabb = SableHelper.toGlobalAABB(subLevel, aabb);
        }

        return camera.isVisible(aabb);
    }

    @Override
    public void render(@NotNull ChainKnotEntity entity, float yaw, float tickDelta, @NotNull PoseStack matrices, @NotNull MultiBufferSource buffers, int light) {
        ChainKnotEntityRenderState state = reusableState;
        fillRenderState(entity, state, tickDelta);

        if (SableHelper.distanceToCameraSqr(entity, this.entityRenderDispatcher) <= 4096.0D) {
            matrices.pushPose();
            transformToKnot(matrices, state);
            VertexConsumer vertexConsumer = buffers.getBuffer(getKnotRenderType(state.sourceItem));
            this.model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, state.knotTintColor);
            matrices.popPose();
        }

        for (ChainKnotEntityRenderState.ChainData chainData : state.chainDataSet) {
            renderChainLink(matrices, buffers, chainData);
        }

        if (CommonClass.runtimeConfig.doDebugDraw()) {
            matrices.pushPose();
            this.renderNameTag(entity, Component.literal("C: " + state.chainDataSet.size()), matrices, buffers, light, tickDelta);
            matrices.popPose();
        }

        super.render(entity, yaw, tickDelta, matrices, buffers, light);
    }

    private void renderChainLink(PoseStack matrices, MultiBufferSource buffers, ChainKnotEntityRenderState.ChainData chainData) {
        boolean debugDraw = CommonClass.runtimeConfig.doDebugDraw();
        RenderType chainType = debugDraw ? RenderType.lines() : getChainRenderType(chainData.sourceItem);
        Vector3f chainVec = chainData.endPos.subtract(chainData.startPos).toVector3f();

        matrices.pushPose();
        matrices.translate(chainData.offset.x, chainData.offset.y, chainData.offset.z);

        PoseStack.Pose pose = matrices.last();
        renderChain(buffers.getBuffer(chainType), pose, chainVec, chainData);
        if (debugDraw) {
            drawDebugVector(pose.pose(), chainData.startPos, chainData.endPos, buffers.getBuffer(RenderType.lines()));
        }

        matrices.mulPose(new Quaternionf().rotationY(yawOf(chainVec)));
        renderBuntingsAlongChain(matrices, buffers, chainVec, chainData);
        renderBannersAlongChain(matrices, buffers, chainVec, chainData);
        renderHangingsAlongChain(matrices, buffers, chainVec, chainData);
        matrices.popPose();
    }

    private void renderBuntingsAlongChain(PoseStack matrices, MultiBufferSource buffers, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        if (chainData.buntings.isEmpty()) return;
        ChainCurve curve = ChainCurve.of(chainVec, chainData.slack);
        Level level = Minecraft.getInstance().level;
        if (curve == null || level == null) return;
        long gameTime = level.getGameTime();

        for (Chainable.ChainData.BuntingEntry entry : chainData.buntings) {
            if (buntingItem(entry.color()) == Items.AIR) continue;

            float x = curve.x(entry.t());
            float y = curve.y(x);
            float pitch = (float) Math.atan2(curve.slope(x), 1.0);
            int light = lerpLight(chainData, entry.t());
            BlockPos buntingPos = BlockPos.containing(chainData.startPos.lerp(chainData.endPos, entry.t()));

            matrices.pushPose();
            matrices.translate(x, y, 0);
            matrices.mulPose(new Quaternionf().rotateZ(pitch));
            matrices.translate(0.25f, -0.19f, 0.0f);
            SupplementariesCompat.renderBunting(entry.color(), matrices, buffers, light, buntingPos, gameTime);
            matrices.popPose();
        }
    }

    private void renderBannersAlongChain(PoseStack matrices, MultiBufferSource buffers, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        if (chainData.banners.isEmpty()) return;
        ChainCurve curve = ChainCurve.of(chainVec, chainData.slack);
        Level level = Minecraft.getInstance().level;
        if (curve == null || level == null) return;

        for (Chainable.ChainData.BannerEntry entry : chainData.banners) {
            float x = curve.x(entry.t());
            float y = curve.y(x);
            int light = lerpLight(chainData, entry.t());
            BannerPatternLayers patterns = bannerPatterns(level, entry.data());

            renderBannerConnector(matrices.last(), buffers.getBuffer(BANNER_CONNECTOR_RENDER_TYPE), light, x, y);

            matrices.pushPose();
            transformToBanner(matrices, x, y);
            BannerRenderer.renderPatterns(matrices, buffers, light, OverlayTexture.NO_OVERLAY, bannerFlag, ModelBakery.BANNER_BASE, true, entry.color(), patterns);
            matrices.translate(0, 0, -0.07);
            matrices.mulPose(new Quaternionf().rotateY((float) Math.PI));
            matrices.translate(0, 0, 0.07);
            BannerRenderer.renderPatterns(matrices, buffers, light, OverlayTexture.NO_OVERLAY, bannerFlag, ModelBakery.BANNER_BASE, true, entry.color(), patterns);
            matrices.popPose();
        }
    }
*///?}

    private void fillRenderState(ChainKnotEntity entity, ChainKnotEntityRenderState state, float tickDelta) {
        Level level = entity.level();
        discardStateOnLevelChange(level);

        state.attachedFace = entity.attachedFace;
        state.sourceItem = entity.getSourceItem();
        state.knotScaleXZ = entity.getKnotScale();
        state.knotTintColor = tintColor(level, state.sourceItem, entity.blockPosition());
        state.reset();

        Vec3 entityPos = entity.getPosition(tickDelta);
        Vec3 srcPos = entity.getChainPos(tickDelta);
        int startLight = this.getPackedLightCoords(entity, tickDelta);

        for (Chainable.ChainData chainData : entity.getChainDataArray()) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder == null) continue;

            Vec3 dstPos = chainHolder instanceof ChainKnotEntity knot
                    ? knot.getChainPos(tickDelta)
                    : chainHolder.getRopeHoldPosition(tickDelta);
            //? if <26.1 {
            /*dstPos = SableHelper.transformHolderPosForRenderer(level, entity, chainHolder, dstPos, tickDelta);
            *///?}
            if (!Chainable.isValidChainDistance(srcPos, dstPos)) continue;

            int endLight = this.entityRenderDispatcher.getPackedLightCoords(chainHolder, tickDelta);

            ChainKnotEntityRenderState.ChainData data = state.claim();
            data.offset = srcPos.subtract(entityPos);
            data.startPos = srcPos;
            data.endPos = dstPos;
            //? if >=26.1 {
            data.chainedEntityBlockLight = LightCoordsUtil.block(startLight);
            data.chainHolderBlockLight = LightCoordsUtil.block(endLight);
            data.chainedEntitySkyLight = LightCoordsUtil.sky(startLight);
            data.chainHolderSkyLight = LightCoordsUtil.sky(endLight);
            //?} else {
            /*data.chainedEntityBlockLight = LightTexture.block(startLight);
            data.chainHolderBlockLight = LightTexture.block(endLight);
            data.chainedEntitySkyLight = LightTexture.sky(startLight);
            data.chainHolderSkyLight = LightTexture.sky(endLight);
            *///?}
            data.sourceItem = chainData.sourceItem;
            data.tintColor = tintColor(level, chainData.sourceItem, BlockPos.containing(srcPos.lerp(dstPos, 0.5)));
            data.useBaked = chainHolder instanceof HangingEntity;
            data.slack = chainData.getSlack();
            data.buntings = List.copyOf(chainData.buntings);
            data.banners = List.copyOf(chainData.banners);
            data.hangings = List.copyOf(chainData.hangings);
        }
    }

    private static void transformToKnot(PoseStack poseStack, ChainKnotEntityRenderState state) {
        if (state.attachedFace != null) {
            switch (state.attachedFace) {
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
    }

    private static void transformToBanner(PoseStack poseStack, float x, float y) {
        poseStack.translate(x, y, 0);
        poseStack.mulPose(new Quaternionf().rotateX((float) Math.PI));
        poseStack.scale(0.66f, 0.66f, 0.66f);
        poseStack.translate(0, 0.375, 0.07);
    }

    private void renderChain(VertexConsumer buffer, PoseStack.Pose pose, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        CatenaryRenderer renderer = getTextureManager().getCatenaryRenderer(chainData.sourceItem);
        if (chainData.useBaked) {
            chainRenderer.renderBaked(renderer, buffer, pose, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight, chainData.tintColor);
        } else {
            chainRenderer.render(renderer, buffer, pose, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight, chainData.tintColor);
        }
    }

//? if >=26.1 {
    private void renderHangingsAlongChain(PoseStack poseStack, SubmitNodeCollector out, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
//?} else {
    /*private void renderHangingsAlongChain(PoseStack poseStack, MultiBufferSource out, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
*///?}
        if (chainData.hangings.isEmpty()) return;
        ChainCurve curve = ChainCurve.of(chainVec, chainData.slack);
        Minecraft mc = Minecraft.getInstance();
        if (curve == null || mc.level == null) return;

        for (Chainable.ChainData.HangingEntry entry : chainData.hangings) {
            float t = entry.t();
            float x = curve.x(t);
            float y = curve.y(x);

            double worldX = Mth.lerp(t, chainData.startPos.x(), chainData.endPos.x());
            double worldY = chainData.startPos.y() + y;
            double worldZ = Mth.lerp(t, chainData.startPos.z(), chainData.endPos.z());
            BlockPos pos = BlockPos.containing(worldX, worldY - 1.0, worldZ);

            hangingBlockRenderer.render(mc, entry.blockId(), pos, poseStack, out, x, y);
        }
    }

    private static void renderBannerConnector(PoseStack.Pose pose, VertexConsumer vc, int light, float x, float y) {
        float hw = 6.0f / 16.0f;
        float h = 6.0f / 16.0f;
        float u1 = 12.0f / 16.0f;
        float v1 = 6.0f / 16.0f;
        float zF = 0.005f, zB = -0.005f;

        connectorVertex(vc, pose, x - hw, y, zF, 0, 0, light, 1);
        connectorVertex(vc, pose, x - hw, y - h, zF, 0, v1, light, 1);
        connectorVertex(vc, pose, x + hw, y - h, zF, u1, v1, light, 1);
        connectorVertex(vc, pose, x + hw, y, zF, u1, 0, light, 1);

        connectorVertex(vc, pose, x + hw, y, zB, 0, 0, light, -1);
        connectorVertex(vc, pose, x + hw, y - h, zB, 0, v1, light, -1);
        connectorVertex(vc, pose, x - hw, y - h, zB, u1, v1, light, -1);
        connectorVertex(vc, pose, x - hw, y, zB, u1, 0, light, -1);
    }

    private static void connectorVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, float normalZ) {
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 0, normalZ);
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }

    public void onResourceReload() {
        bannerPatternCache.clear();
        hangingBlockRenderer.clear();
        chainRenderer.purge();
        chainRenderTypes.clear();
        knotRenderTypes.clear();
    }

    private void discardStateOnLevelChange(Level level) {
        if (lastLevel == level) return;
        lastLevel = level;
        chainRenderer.purge();
        hangingBlockRenderer.clear();
        bannerPatternCache.clear();
    }

    private static float yawOf(Vector3f chainVec) {
        return -(float) Math.atan2(chainVec.z(), chainVec.x());
    }

    private static int lerpLight(ChainKnotEntityRenderState.ChainData chainData, float t) {
        int blockLight = (int) Mth.lerp(t, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight);
        int skyLight = (int) Mth.lerp(t, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
        //? if >=26.1 {
        return LightCoordsUtil.pack(blockLight, skyLight);
        //?} else {
        /*return LightTexture.pack(blockLight, skyLight);
        *///?}
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

    private static void drawDebugVector(Matrix4f matrix, Vec3 startPos, Vec3 endPos, VertexConsumer buffer) {
        Vec3 vec = endPos.subtract(startPos);
        float yaw = -(float) Math.atan2(vec.z(), vec.x());
        float cos = (float) Math.cos(yaw);
        float sin = (float) Math.sin(yaw);
        float x = (float) (vec.x * cos + vec.z * sin);
        float z = (float) (-vec.x * sin + vec.z * cos);
        Vec3 normal = vec.normalize();

        debugVertex(buffer, matrix, 0, 0, 0, 0, 255, 0, normal);
        debugVertex(buffer, matrix, x, (float) vec.y, z, 255, 0, 0, normal);
    }

    private static void debugVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int r, int g, int b, Vec3 normal) {
        Vector4f vector = new Vector4f(x, y, z, 1.0F).mul(matrix);
        buffer.addVertex(vector.x, vector.y, vector.z)
                .setColor(r, g, b, 255)
                //? if >=26.1 {
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z)
                .setLineWidth(1.0F);
                //?} else {
                /*.setNormal((float) normal.x, (float) normal.y, (float) normal.z);
                *///?}
    }

    private static ChainTextureManager getTextureManager() {
        return ClientInitializer.getInstance().getChainTextureManager();
    }

    private RenderType getChainRenderType(Item item) {
        //? if >=26.1 {
        return chainRenderTypes.computeIfAbsent(item, i -> RenderTypes.entityCutout(getTextureManager().getChainTexture(i)));
        //?} else {
        /*return chainRenderTypes.computeIfAbsent(item, i -> RenderType.entityCutoutNoCull(getTextureManager().getChainTexture(i)));
        *///?}
    }

    private RenderType getKnotRenderType(Item item) {
        //? if >=26.1 {
        return knotRenderTypes.computeIfAbsent(item, i -> RenderTypes.entityCutoutCull(getTextureManager().getKnotTexture(i)));
        //?} else {
        /*return knotRenderTypes.computeIfAbsent(item, i -> RenderType.entityCutout(getTextureManager().getKnotTexture(i)));
        *///?}
    }

    private static int tintColor(Level level, Item item, BlockPos pos) {
        return getTextureManager().getTint(item)
                .map(tint -> 0xFF000000 | sampleBiomeColor(level, tint, pos))
                .orElse(NO_TINT);
    }

    private static int sampleBiomeColor(Level level, String tintType, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)) return DEFAULT_BIOME_COLOR;
        return switch (tintType) {
            case "foliage" -> BiomeColors.getAverageFoliageColor(clientLevel, pos);
            case "grass" -> BiomeColors.getAverageGrassColor(clientLevel, pos);
            case "water" -> BiomeColors.getAverageWaterColor(clientLevel, pos);
            default -> DEFAULT_BIOME_COLOR;
        };
    }

    private record ChainCurve(float distanceXZ, float stretch, MathHelper.Catenary catenary) {
        static @Nullable ChainCurve of(Vector3f chainVec, float slack) {
            float distanceXZ = (float) Math.sqrt(chainVec.x() * chainVec.x() + chainVec.z() * chainVec.z());
            if (distanceXZ < 0.1f) return null;
            float distance = chainVec.length();
            return new ChainCurve(distanceXZ, distance / distanceXZ, MathHelper.Catenary.of(distance, chainVec.y(), slack));
        }

        float x(float t) {
            return t * distanceXZ;
        }

        float y(float x) {
            return (float) catenary.y(x * stretch);
        }

        float slope(float x) {
            return (float) (catenary.slope(x * stretch) * stretch);
        }
    }
}
