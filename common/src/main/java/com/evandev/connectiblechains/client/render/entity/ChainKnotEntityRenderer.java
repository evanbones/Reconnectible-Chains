package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.evandev.connectiblechains.client.render.entity.state.ChainKnotEntityRenderState;
import com.evandev.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.ChainTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;

public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity, ChainKnotEntityRenderState> {
    private final ChainKnotEntityModel model;
    private final ChainRenderer chainRenderer = new ChainRenderer();

    public ChainKnotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel(context.bakeLayer(ClientInitializer.CHAIN_KNOT));
        ClientInitializer.getInstance().setChainKnotEntityRenderer(this);
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }

    @Override
    public @NotNull ChainKnotEntityRenderState createRenderState() {
        return new ChainKnotEntityRenderState();
    }

    @Override
    public void extractRenderState(ChainKnotEntity entity, ChainKnotEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);

        state.attachedFace = entity.attachedFace;
        state.sourceItem = entity.getSourceItem();

        state.scaleXZ = 5 / 6f;
        Level level = entity.level();
        BlockState blockState = level.getBlockState(entity.blockPosition());
        VoxelShape shape = blockState.getShape(level, entity.blockPosition());

        if (!shape.isEmpty()) {
            double lx = entity.getX() - Math.floor(entity.getX());
            double ly = entity.getY() - Math.floor(entity.getY());
            double lz = entity.getZ() - Math.floor(entity.getZ());

            double push = 0.05;
            lx -= state.attachedFace.getStepX() * push;
            ly -= state.attachedFace.getStepY() * push;
            lz -= state.attachedFace.getStepZ() * push;

            AABB attachmentPoint = new AABB(lx - 0.05, ly - 0.05, lz - 0.05, lx + 0.05, ly + 0.05, lz + 0.05);
            AABB bestBox = null;

            for (AABB box : shape.toAabbs()) {
                if (box.intersects(attachmentPoint)) {
                    bestBox = box;
                    break;
                }
            }
            if (bestBox == null) bestBox = shape.bounds();

            double dim1 = 0, dim2 = 0;
            switch (state.attachedFace.getAxis()) {
                case Y -> {
                    dim1 = bestBox.getXsize();
                    dim2 = bestBox.getZsize();
                }
                case Z -> {
                    dim1 = bestBox.getXsize();
                    dim2 = bestBox.getYsize();
                }
                case X -> {
                    dim1 = bestBox.getYsize();
                    dim2 = bestBox.getZsize();
                }
            }

            double minDim = Math.min(dim1, dim2);
            state.scaleXZ = Math.max(0.5f, Math.min(1.5f, (float) (minDim + 0.0625) / 0.375f));
        }

        HashSet<ChainKnotEntityRenderState.ChainData> result = new HashSet<>();
        Vec3 entityPos = entity.getPosition(tickDelta);

        for (Chainable.ChainData chainData : new HashSet<>(entity.getChainDataSet())) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder == null) continue;

            Vec3 srcPos = entity.getChainPos(tickDelta);
            Vec3 dstPos = (chainHolder instanceof ChainKnotEntity chainKnotEntity)
                    ? chainKnotEntity.getChainPos(tickDelta)
                    : chainHolder.getRopeHoldPosition(tickDelta);

            BlockPos blockPosOfStart = BlockPos.containing(entity.getEyePosition(tickDelta));
            BlockPos blockPosOfEnd = BlockPos.containing(chainHolder.getEyePosition(tickDelta));

            ChainKnotEntityRenderState.ChainData renderChainData = new ChainKnotEntityRenderState.ChainData();
            renderChainData.offset = srcPos.subtract(entityPos);
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

        for (Chainable other : ChainTracker.getChains(level)) {
            if (other == entity) continue;
            Chainable.ChainData incomingLink = other.getChainData(entity);
            if (incomingLink != null) {
                Entity otherEntity = (Entity) other;
                Vec3 srcPos = other.getChainPos(tickDelta);
                Vec3 dstPos = entity.getChainPos(tickDelta);

                BlockPos blockPosOfStart = BlockPos.containing(otherEntity.getEyePosition(tickDelta));
                BlockPos blockPosOfEnd = BlockPos.containing(entity.getEyePosition(tickDelta));

                ChainKnotEntityRenderState.ChainData renderChainData = new ChainKnotEntityRenderState.ChainData();
                renderChainData.offset = srcPos.subtract(entityPos);
                renderChainData.startPos = srcPos;
                renderChainData.endPos = dstPos;
                renderChainData.chainedEntityBlockLight = level.getBrightness(LightLayer.BLOCK, blockPosOfStart);
                renderChainData.chainHolderBlockLight = level.getBrightness(LightLayer.BLOCK, blockPosOfEnd);
                renderChainData.chainedEntitySkyLight = level.getBrightness(LightLayer.SKY, blockPosOfStart);
                renderChainData.chainHolderSkyLight = level.getBrightness(LightLayer.SKY, blockPosOfEnd);
                renderChainData.sourceItem = incomingLink.sourceItem;
                renderChainData.useBaked = otherEntity instanceof HangingEntity;
                renderChainData.slack = incomingLink.getSlack();
                result.add(renderChainData);
            }
        }

        state.chainDataSet = result;

        if (CommonClass.runtimeConfig.doDebugDraw()) {
            state.nameTag = Component.literal("C: " + state.chainDataSet.size());
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
        poseStack.scale(state.scaleXZ, 1, state.scaleXZ);

        RenderType knotType = this.model.renderType(getKnotTexture(state.sourceItem));

        collector.submitCustomGeometry(poseStack, knotType, (pose, vertexConsumer) -> {
            PoseStack tempStack = new PoseStack();
            tempStack.last().pose().set(pose.pose());
            tempStack.last().normal().set(pose.normal());
            this.model.renderToBuffer(tempStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        });
        poseStack.popPose();

        HashSet<ChainKnotEntityRenderState.ChainData> chainDataSet = state.chainDataSet;
        for (ChainKnotEntityRenderState.ChainData chainData : chainDataSet) {
            RenderType entityCutout = CommonClass.runtimeConfig.doDebugDraw() ? RenderTypes.lines() : RenderTypes.entityCutout(getChainTexture(chainData.sourceItem));

            poseStack.pushPose();
            collector.submitCustomGeometry(poseStack, entityCutout, (pose, vertexConsumer) -> {
                PoseStack tempStack = new PoseStack();
                tempStack.last().pose().set(pose.pose());
                tempStack.last().normal().set(pose.normal());
                renderChainLink(tempStack, vertexConsumer, chainData);

                if (CommonClass.runtimeConfig.doDebugDraw()) {
                    this.drawDebugVector(tempStack, chainData.startPos, chainData.endPos, vertexConsumer);
                }
            });
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void renderChainLink(PoseStack poseStack, VertexConsumer vertexConsumer, ChainKnotEntityRenderState.ChainData chainData) {
        Vec3 offset = chainData.offset;
        Vec3 startPos = chainData.startPos;
        Vec3 endPos = chainData.endPos;
        Item sourceItem = chainData.sourceItem;

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);

        Vector3f chainVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));
        float angleY = -(float) Math.atan2(chainVec.z(), chainVec.x());
        poseStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        CatenaryRenderer renderer = getCatenaryRenderer(sourceItem);

        if (chainData.useBaked) {
            chainRenderer.renderBaked(renderer, vertexConsumer, poseStack, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
        } else {
            chainRenderer.render(renderer, vertexConsumer, poseStack, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
        }

        poseStack.popPose();
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
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return getTextureManager().getCatenaryRenderer(id);
    }
}