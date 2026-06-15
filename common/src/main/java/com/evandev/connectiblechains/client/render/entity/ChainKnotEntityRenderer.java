package com.evandev.connectiblechains.client.render.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.SupplementariesCompat;
import com.evandev.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.evandev.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.evandev.connectiblechains.client.render.entity.state.ChainKnotEntityRenderState;
import com.evandev.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.util.ChainTracker;
import com.evandev.connectiblechains.util.MathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;

public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private static final ResourceLocation BANNER_CONNECTOR_TEXTURE =
            new ResourceLocation(CommonClass.MODID, "textures/block/banner_connector.png");

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
        double distanceToCameraSqr = this.entityRenderDispatcher.distanceToSqr(entity);

        if (distanceToCameraSqr <= 4096.0D) {
            matrices.pushPose();
            Direction face = entity.attachedFace;

            switch (face) {
                case DOWN -> matrices.mulPose(new Quaternionf().rotateX((float) Math.PI));
                case NORTH -> matrices.mulPose(new Quaternionf().rotateX((float) -Math.PI / 2f));
                case SOUTH -> matrices.mulPose(new Quaternionf().rotateX((float) Math.PI / 2f));
                case WEST -> matrices.mulPose(new Quaternionf().rotateZ((float) Math.PI / 2f));
                case EAST -> matrices.mulPose(new Quaternionf().rotateZ((float) -Math.PI / 2f));
                case UP -> {
                }
            }

            matrices.translate(0, 0.7, 0);

            float scaleXZ = 5 / 6f;
            BlockState blockState = entity.level().getBlockState(entity.getPos());
            VoxelShape shape = blockState.getShape(entity.level(), entity.getPos());

            if (!shape.isEmpty()) {
                double lx = entity.getX() - Math.floor(entity.getX());
                double ly = entity.getY() - Math.floor(entity.getY());
                double lz = entity.getZ() - Math.floor(entity.getZ());

                double push = 0.05;
                lx -= face.getStepX() * push;
                ly -= face.getStepY() * push;
                lz -= face.getStepZ() * push;

                AABB attachmentPoint = new AABB(lx - 0.05, ly - 0.05, lz - 0.05, lx + 0.05, ly + 0.05, lz + 0.05);
                AABB bestBox = null;

                for (AABB box : shape.toAabbs()) {
                    if (box.intersects(attachmentPoint)) {
                        bestBox = box;
                        break;
                    }
                }

                if (bestBox == null) {
                    bestBox = shape.bounds();
                }

                double dim1 = 0, dim2 = 0;
                switch (face.getAxis()) {
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

                scaleXZ = Math.max(0.5f, Math.min(1.5f, (float) (minDim + 0.0625) / 0.375f));
            }

            matrices.scale(scaleXZ, 1, scaleXZ);

            int knotTint = state.knotTintColor;
            float kr = ((knotTint >> 16) & 0xFF) / 255.0f;
            float kg = ((knotTint >> 8) & 0xFF) / 255.0f;
            float kb = (knotTint & 0xFF) / 255.0f;
            float ka = ((knotTint >> 24) & 0xFF) / 255.0f;
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.renderType(getKnotTexture(state.sourceItem)));
            this.model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, kr, kg, kb, ka);
            matrices.popPose();
        }

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
            chainRenderer.renderBaked(renderer, vertexConsumer, matrices, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight, chainData.tintColor);
        } else {
            chainRenderer.render(renderer, vertexConsumer, matrices, chainVec, chainData.slack, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight, chainData.tintColor);
        }

        if (!chainData.buntings.isEmpty()) {
            renderBuntingsAlongChain(matrices, vertexConsumerProvider, chainVec, chainData);
        }

        if (!chainData.banners.isEmpty()) {
            renderBannersAlongChain(matrices, vertexConsumerProvider, chainVec, chainData);
        }

        if (!chainData.hangings.isEmpty()) {
            renderHangingsAlongChain(matrices, vertexConsumerProvider, chainVec, chainData);
        }

        matrices.popPose();
    }

    private void renderBuntingsAlongChain(PoseStack matrices, MultiBufferSource buffers, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        float distanceXZ = (float) Math.sqrt(chainVec.x() * chainVec.x() + chainVec.z() * chainVec.z());
        if (distanceXZ < 0.1f) return;

        float distance = chainVec.length();
        float wrongDistanceFactor = distance / distanceXZ;
        float slack = chainData.slack;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long gameTime = mc.level.getGameTime();

        for (Chainable.ChainData.BuntingEntry entry : chainData.buntings) {
            float t = entry.t();
            float x = t * distanceXZ;
            float y = (float) MathHelper.drip2(x * wrongDistanceFactor, distance, chainVec.y(), slack);

            float slope = (float) (MathHelper.drip2prime(x * wrongDistanceFactor, distance, chainVec.y(), slack) * wrongDistanceFactor);
            float pitchRad = (float) Math.atan2(slope, 1.0);

            int blockLight = (int) Mth.lerp(t, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight);
            int skyLight = (int) Mth.lerp(t, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
            int light = LightTexture.pack(blockLight, skyLight);

            BlockPos buntingBlockPos = BlockPos.containing(chainData.startPos.lerp(chainData.endPos, t));

            matrices.pushPose();
            matrices.translate(x, y, 0);
            matrices.mulPose(new Quaternionf().rotateZ(pitchRad));
            matrices.translate(0.25f, -0.19f, 0.0f);
            SupplementariesCompat.renderBunting(entry.color(), matrices, buffers, light, buntingBlockPos, gameTime);
            matrices.popPose();
        }
    }

    private void renderBannersAlongChain(PoseStack matrices, MultiBufferSource buffers, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        float distanceXZ = (float) Math.sqrt(chainVec.x() * chainVec.x() + chainVec.z() * chainVec.z());
        if (distanceXZ < 0.1f) return;

        float distance = chainVec.length();
        float wrongDistanceFactor = distance / distanceXZ;
        float slack = chainData.slack;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        ModelPart flag = mc.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");

        for (Chainable.ChainData.BannerEntry entry : chainData.banners) {
            float t = entry.t();
            float x = t * distanceXZ;
            float y = (float) MathHelper.drip2(x * wrongDistanceFactor, distance, chainVec.y(), slack);

            int blockLight = (int) Mth.lerp(t, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight);
            int skyLight = (int) Mth.lerp(t, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
            int light = LightTexture.pack(blockLight, skyLight);

            ListTag patterns = entry.data().getList("Patterns", Tag.TAG_COMPOUND);
            var patternList = BannerBlockEntity.createPatterns(entry.color(), patterns);

            renderBannerConnector(matrices, buffers, light, x, y);

            matrices.pushPose();
            matrices.translate(x, y, 0);
            matrices.mulPose(new Quaternionf().rotateX((float) Math.PI));
            matrices.scale(0.66f, 0.66f, 0.66f);

            matrices.translate(0, 0.375, 0.07);
            BannerRenderer.renderPatterns(matrices, buffers, light, OverlayTexture.NO_OVERLAY, flag, ModelBakery.BANNER_BASE, true, patternList);
            matrices.translate(0, 0, -0.07);
            matrices.mulPose(new Quaternionf().rotateY((float) Math.PI));
            matrices.translate(0, 0, 0.07);
            BannerRenderer.renderPatterns(matrices, buffers, light, OverlayTexture.NO_OVERLAY, flag, ModelBakery.BANNER_BASE, true, patternList);

            matrices.popPose();
        }
    }

    private void renderHangingsAlongChain(PoseStack matrices, MultiBufferSource buffers, Vector3f chainVec, ChainKnotEntityRenderState.ChainData chainData) {
        float distanceXZ = (float) Math.sqrt(chainVec.x() * chainVec.x() + chainVec.z() * chainVec.z());
        if (distanceXZ < 0.1f) return;

        float distance = chainVec.length();
        float wrongDistanceFactor = distance / distanceXZ;
        float slack = chainData.slack;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();

        for (Chainable.ChainData.HangingEntry entry : chainData.hangings) {
            float t = entry.t();
            float x = t * distanceXZ;
            float y = (float) MathHelper.drip2(x * wrongDistanceFactor, distance, chainVec.y(), slack);

            int blockLight = (int) Mth.lerp(t, chainData.chainedEntityBlockLight, chainData.chainHolderBlockLight);
            int skyLight = (int) Mth.lerp(t, chainData.chainedEntitySkyLight, chainData.chainHolderSkyLight);
            int light = LightTexture.pack(blockLight, skyLight);

            Block block = BuiltInRegistries.BLOCK.get(entry.blockId());
            if (block == Blocks.AIR) continue;
            Item item = BuiltInRegistries.ITEM.get(entry.blockId());

            BlockState blockState = block.defaultBlockState();
            if (blockState.hasProperty(BlockStateProperties.HANGING)) {
                blockState = blockState.setValue(BlockStateProperties.HANGING, true);
            }

            matrices.pushPose();
            if (blockState.getRenderShape() == RenderShape.MODEL) {
                matrices.translate(x - 0.5f, y - 1.0f, -0.5f);
                blockRenderer.renderSingleBlock(blockState, matrices, buffers, light, OverlayTexture.NO_OVERLAY);
            } else if (item != Items.AIR) {
                matrices.translate(x, y - 0.5f, 0f);
                matrices.scale(0.5f, 0.5f, 0.5f);
                mc.getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.FIXED,
                        light, OverlayTexture.NO_OVERLAY, matrices, buffers, mc.level, 0);
            }
            matrices.popPose();
        }
    }

    private void renderBannerConnector(PoseStack matrices, MultiBufferSource buffers, int light, float x, float y) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(BANNER_CONNECTOR_TEXTURE));
        Matrix4f pose = matrices.last().pose();
        Matrix3f normal = matrices.last().normal();
        float hw = 6.0f / 16.0f;
        float h = 6.0f / 16.0f;
        float u1 = 12.0f / 16.0f;
        float v1 = 6.0f / 16.0f;
        float zF = 0.005f, zB = -0.005f;

        // Front face
        vc.vertex(pose, x - hw, y, zF).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();
        vc.vertex(pose, x - hw, y - h, zF).color(255, 255, 255, 255).uv(0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();
        vc.vertex(pose, x + hw, y - h, zF).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();
        vc.vertex(pose, x + hw, y, zF).color(255, 255, 255, 255).uv(u1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();

        // Back face
        vc.vertex(pose, x + hw, y, zB).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();
        vc.vertex(pose, x + hw, y - h, zB).color(255, 255, 255, 255).uv(0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();
        vc.vertex(pose, x - hw, y - h, zB).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();
        vc.vertex(pose, x - hw, y, zB).color(255, 255, 255, 255).uv(u1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();
    }

    private void drawDebugVector(PoseStack matrices, Vec3 startPos, Vec3 endPos, VertexConsumer buffer) {
        if (startPos == null) return;
        Matrix4f matrix = matrices.last().pose();
        Matrix3f normalMatrix = matrices.last().normal();
        Vec3 vec = endPos.subtract(startPos);
        Vec3 n = vec.normalize();
        buffer.vertex(matrix, 0, 0, 0).color(0, 255, 0, 255).normal(normalMatrix, (float) n.x, (float) n.y, (float) n.z).endVertex();
        buffer.vertex(matrix, (float) vec.x, (float) vec.y, (float) vec.z).color(255, 0, 0, 255).normal(normalMatrix, (float) n.x, (float) n.y, (float) n.z).endVertex();
    }

    public void updateRenderState(ChainKnotEntity entity, ChainKnotEntityRenderState state, float tickDelta) {
        HashSet<ChainKnotEntityRenderState.ChainData> result = new HashSet<>();
        Level level = entity.level();
        Vec3 entityPos = entity.getPosition(tickDelta);

        for (Chainable.ChainData chainData : new HashSet<>(entity.getChainDataSet())) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder == null) continue;

            Vec3 srcPos = entity.getChainPos(tickDelta);
            Vec3 dstPos = chainHolder instanceof ChainKnotEntity knot ? knot.getChainPos(tickDelta) : chainHolder.getRopeHoldPosition(tickDelta);

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
            renderChainData.tintColor = computeChainTintColor(level, chainData.sourceItem, srcPos, dstPos);
            renderChainData.useBaked = chainHolder instanceof HangingEntity;
            renderChainData.slack = chainData.getSlack();
            renderChainData.buntings = new ArrayList<>(chainData.buntings);
            renderChainData.banners = new ArrayList<>(chainData.banners);
            renderChainData.hangings = new ArrayList<>(chainData.hangings);
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
                renderChainData.tintColor = computeChainTintColor(level, incomingLink.sourceItem, srcPos, dstPos);
                renderChainData.useBaked = otherEntity instanceof HangingEntity;
                renderChainData.slack = incomingLink.getSlack();
                renderChainData.buntings = new ArrayList<>(incomingLink.buntings);
                renderChainData.banners = new ArrayList<>(incomingLink.banners);
                renderChainData.hangings = new ArrayList<>(incomingLink.hangings);
                result.add(renderChainData);
            }
        }

        state.chainDataSet = result;
        state.sourceItem = entity.getSourceItem();
        state.knotTintColor = computeKnotTintColor(level, entity.getSourceItem(), entity.blockPosition());
    }

    private ChainTextureManager getTextureManager() {
        return ClientInitializer.getInstance().getChainTextureManager();
    }

    private ResourceLocation getKnotTexture(Item item) {
        return getTextureManager().getKnotTexture(item);
    }

    private ResourceLocation getChainTexture(Item item) {
        return getTextureManager().getChainTexture(item);
    }

    private CatenaryRenderer getCatenaryRenderer(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return getTextureManager().getCatenaryRenderer(id);
    }

    private int computeChainTintColor(Level level, Item sourceItem, Vec3 srcPos, Vec3 dstPos) {
        return getTextureManager().getTint(sourceItem)
                .map(tint -> 0xFF000000 | sampleBiomeColor(level, tint, BlockPos.containing(srcPos.lerp(dstPos, 0.5))))
                .orElse(0xFFCCCCCC);
    }

    private int computeKnotTintColor(Level level, Item sourceItem, BlockPos pos) {
        return getTextureManager().getTint(sourceItem)
                .map(tint -> 0xFF000000 | sampleBiomeColor(level, tint, pos))
                .orElse(0xFFFFFFFF);
    }

    private int sampleBiomeColor(Level level, String tintType, BlockPos pos) {
        return switch (tintType) {
            case "foliage" -> BiomeColors.getAverageFoliageColor(level, pos);
            case "grass" -> BiomeColors.getAverageGrassColor(level, pos);
            case "water" -> BiomeColors.getAverageWaterColor(level, pos);
            default -> 0xCCCCCC;
        };
    }
}
