package com.mkzv.mkzvcraft.client.entity;

import com.mkzv.mkzvcraft.MkzvCraft;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Leather pouch worn across the body: the bag sits on the front of the belly, the strap goes
 * over the right shoulder. Coordinates are in the player's body space (pivot at the neck,
 * body box is x -4..4, y 0..12, z -2..2). Texture is 64x32.
 */
public class GroguPouchModel extends EntityModel<GroguRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(MkzvCraft.id("grogu_pouch"), "main");

	/** Centre of the bag opening, where Grogu sits (body space, pixels). */
	public static final float BAG_CENTER_X = 0.5F;
	public static final float BAG_CENTER_Z = -5.2F;

	public GroguPouchModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("bag",
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0.0F, -6.0F, 7.0F, 6.0F, 6.0F),
				PartPose.offset(BAG_CENTER_X, 6.0F, -2.2F));
		// Right shoulder (x -3) down across the chest to the bag.
		root.addOrReplaceChild("front_strap",
				CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 9.0F, 1.0F),
				PartPose.offsetAndRotation(-3.0F, 0.0F, -2.5F, 0.0F, 0.0F, -0.825F));
		// Right shoulder down across the back to the left hip.
		root.addOrReplaceChild("back_strap",
				CubeListBuilder.create().texOffs(8, 14).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 13.0F, 1.0F),
				PartPose.offsetAndRotation(-3.0F, 0.0F, 2.5F, 0.0F, 0.0F, -0.567F));
		root.addOrReplaceChild("shoulder_strap",
				CubeListBuilder.create().texOffs(16, 14).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 1.0F, 5.0F),
				PartPose.offset(-3.0F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}
}
