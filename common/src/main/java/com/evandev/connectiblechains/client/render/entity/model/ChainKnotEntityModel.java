package com.evandev.connectiblechains.client.render.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class ChainKnotEntityModel<T extends Entity> extends EntityModel<T> {
    private static final String KNOT = "knot";
    private final ModelPart root;
    private final ModelPart knot;

    public ChainKnotEntityModel(ModelPart root) {
        this.root = root;
        this.knot = root.getChild(KNOT);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition knot = partdefinition.addOrReplaceChild("knot", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -7F, 0.0F, 0.0F, 0.0F, -3.1416F));

        knot.addOrReplaceChild("south_r1", CubeListBuilder.create().texOffs(-4, 3).mirror().addBox(-2.5F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, 1.5708F, 0.0F, -1.5708F));
        knot.addOrReplaceChild("north_r1", CubeListBuilder.create().texOffs(-4, 3).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.0F, -3.0F, 1.5708F, 0.0F, 1.5708F));
        knot.addOrReplaceChild("east_r1", CubeListBuilder.create().texOffs(-4, 8).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -0.0F, 0.0F, -3.1416F, 0.0F, 1.5708F));
        knot.addOrReplaceChild("west_r1", CubeListBuilder.create().texOffs(-4, 8).mirror().addBox(-2.5F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(@NotNull T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.knot.yRot = netHeadYaw * (float) (Math.PI / 180.0);
        this.knot.xRot = headPitch * (float) (Math.PI / 180.0);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}