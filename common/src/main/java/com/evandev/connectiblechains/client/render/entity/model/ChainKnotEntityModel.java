package com.evandev.connectiblechains.client.render.entity.model;

import com.evandev.connectiblechains.client.render.entity.state.ChainKnotEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class ChainKnotEntityModel extends EntityModel<ChainKnotEntityRenderState> {
    public ChainKnotEntityModel(ModelPart root) {
        super(root);
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
    public void setupAnim(ChainKnotEntityRenderState state) {
    }
}