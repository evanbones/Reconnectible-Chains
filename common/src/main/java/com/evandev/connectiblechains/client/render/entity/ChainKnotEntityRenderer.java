package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.evandev.connectiblechains.client.render.entity.state.ChainKnotEntityRenderState;
import com.evandev.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;

public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private final ChainKnotEntityModel<ChainKnotEntity> model;
    private final ChainRenderer chainRenderer = new ChainRenderer();

    public ChainKnotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel<>(context.bakeLayer(ClientInitializer.CHAIN_KNOT));

        ClientInitializer.getInstance().setChainKnotEntityRenderer(this);
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ChainKnotEntity entity) {
        return null;
    }

    public ChainKnotEntityRenderState createRenderState() {
        return new ChainKnotEntityRenderState();
    }

    @Override
    public void render(@NotNull ChainKnotEntity entity, float yaw, float tickDelta, @NotNull PoseStack matrices, @NotNull MultiBufferSource vertexConsumers, int light) {
        ChainKnotEntityRenderState state = createRenderState();
        updateRenderState(entity, state, tickDelta);
        render(entity, state, matrices, vertexConsumers, light, tickDelta);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    public void render(ChainKnotEntity entity, ChainKnotEntityRenderState state, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float tickDelta) {
        matrices.pushPose();
        matrices.translate(0, 0.7, 0);

        float scaleXZ = 5 / 6f;
        BlockState blockState = entity.level().getBlockState(entity.getPos());
        BlockState defaultState = blockState.getBlock().defaultBlockState();
        VoxelShape shape = defaultState.getShape(entity.level(), entity.getPos());
        if (!shape.isEmpty()) {
            AABB bounds = shape.bounds();
            double maxDim = Math.max(bounds.getXsize(), bounds.getZsize());
            scaleXZ = (float) (maxDim + 0.0625) / 0.375f;
        }
        matrices.scale(scaleXZ, 1, scaleXZ);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.renderType(getKnotTexture(state.sourceItem)));
        this.model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrices.popPose();

        HashSet<ChainKnotEntityRenderState.ChainData> chainDataSet = state.chainDataSet;
        for (ChainKnotEntityRenderState.ChainData chainData : chainDataSet) {
            renderChainLink(matrices, vertexConsumers, chainData);
            if (CommonClass.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(matrices, chainData.startPos, chainData.endPos, vertexConsumers.getBuffer(RenderType.lines()));
            }
        }

        if (CommonClass.runtimeConfig.doDebugDraw()) {
            matrices.pushPose();
            Component holdingCount = Component.literal("C: " + chainDataSet.size());
            this.renderNameTag(entity, holdingCount, matrices, vertexConsumers, light);
            matrices.popPose();
        }
    }

    private void renderChainLink(PoseStack matrices, MultiBufferSource vertexConsumerProvider, ChainKnotEntityRenderState.ChainData chainData) {
        Vec3 offset = chainData.offset;
        Vec3 startPos = chainData.startPos;
        Vec3 endPos = chainData.endPos;
        Item sourceItem = chainData.sourceItem;

        RenderType entityCutout = RenderType.entityCutoutNoCull(getChainTexture(sourceItem));
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(entityCutout);
        if (CommonClass.runtimeConfig.doDebugDraw()) {
            vertexConsumer = vertexConsumerProvider.getBuffer(RenderType.lines());
        }

        matrices.pushPose();
        matrices.translate(offset.x, offset.y, offset.z);

        Vector3f chainVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));
        float angleY = -(float) Math.atan2(chainVec.z(), chainVec.x());
        matrices.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        CatenaryRenderer renderer = getCatenaryRenderer(sourceItem);

        if (chainData.useBaked) {
            chainRenderer.renderBaked(renderer, vertexConsumer, matrices, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
        } else {
            chainRenderer.render(renderer, vertexConsumer, matrices, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
        }

        matrices.popPose();
    }

    private void drawDebugVector(PoseStack matrices, Vec3 startPos, Vec3 endPos, VertexConsumer buffer) {
        if (startPos == null) return;
        var modelMat = matrices.last().pose();
        Vec3 vec = endPos.subtract(startPos);
        Vec3 normal = vec.normalize();
        buffer.vertex(modelMat, 0, 0, 0)
                .color(0, 255, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
        buffer.vertex(modelMat, (float) vec.x, (float) vec.y, (float) vec.z)
                .color(255, 0, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    public void updateRenderState(ChainKnotEntity entity, ChainKnotEntityRenderState state, float tickDelta) {
        HashSet<ChainKnotEntityRenderState.ChainData> result = new HashSet<>(entity.getChainDataSet().size());
        for (Chainable.ChainData chainData : new HashSet<>(entity.getChainDataSet())) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder == null) continue;

            Vec3 offset = new Vec3(0, 0.3, 0);
            Vec3 srcPos = entity.getChainPos(tickDelta);
            Vec3 dstPos;
            if (chainHolder instanceof ChainKnotEntity chainKnotEntity) {
                dstPos = chainKnotEntity.getChainPos(tickDelta);
            } else {
                dstPos = chainHolder.getRopeHoldPosition(tickDelta);
            }

            BlockPos blockPosOfStart = BlockPos.containing(entity.getEyePosition(tickDelta));
            BlockPos blockPosOfEnd = BlockPos.containing(chainHolder.getEyePosition(tickDelta));
            Level level = entity.level();

            ChainKnotEntityRenderState.ChainData renderChainData = new ChainKnotEntityRenderState.ChainData();
            renderChainData.offset = offset;
            renderChainData.startPos = srcPos;
            renderChainData.endPos = dstPos;
            renderChainData.chainedEntityBlockLight = level.getBrightness(LightLayer.BLOCK, blockPosOfStart);
            renderChainData.chainHolderBlockLight = level.getBrightness(LightLayer.BLOCK, blockPosOfEnd);
            renderChainData.chainedEntitySkyLight = level.getBrightness(LightLayer.SKY, blockPosOfStart);
            renderChainData.chainHolderSkyLight = level.getBrightness(LightLayer.SKY, blockPosOfEnd);
            renderChainData.sourceItem = chainData.sourceItem;
            renderChainData.useBaked = chainHolder instanceof HangingEntity;
            renderChainData.slack = chainData.getSlack();
            result.add(renderChainData);
        }
        state.chainDataSet = result;
        state.sourceItem = entity.getSourceItem();
    }

    private ChainTextureManager getTextureManager() {
        return ClientInitializer.getInstance().getChainTextureManager();
    }

    private ResourceLocation getKnotTexture(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return getTextureManager().getKnotTexture(id);
    }

    private ResourceLocation getChainTexture(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return getTextureManager().getChainTexture(id);
    }

    private CatenaryRenderer getCatenaryRenderer(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return getTextureManager().getCatenaryRenderer(id);
    }
}