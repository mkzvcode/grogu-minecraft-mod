package com.mkzv.mkzvcraft.client.entity;

import com.mkzv.mkzvcraft.MkzvCraft;
import com.mkzv.mkzvcraft.entity.Grogu;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Chunky head with long stepped ears, a robe down to the ground with a thick rolled collar,
 * sleeves with big green hands. No visible legs. About 15 px (just under a block) tall.
 * Texture is 64x64; layout matches the boxes below.
 */
public class GroguModel extends EntityModel<GroguRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(MkzvCraft.id("grogu"), "main");

	private static final float DEG = Mth.DEG_TO_RAD;
	private static final float EAR_TILT = 0.18F;
	private static final float EAR_SWEEP = 0.12F;
	private static final float ARM_SPREAD = 0.3F;
	private static final float ARM_FORWARD = -0.35F;

	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public GroguModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.body = root.getChild("body");
		this.rightArm = this.body.getChild("right_arm");
		this.leftArm = this.body.getChild("left_arm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -3.5F, 8.0F, 7.0F, 7.0F),
				PartPose.offset(0.0F, 16.0F, 0.0F));

		// Ears taper in three steps with a straight top edge, so the tip points up once tilted.
		head.addOrReplaceChild("right_ear",
				CubeListBuilder.create()
						.texOffs(32, 0).addBox(-4.0F, -1.5F, -0.5F, 4.0F, 3.0F, 1.0F)
						.texOffs(32, 4).addBox(-7.0F, -1.5F, -0.5F, 3.0F, 2.0F, 1.0F)
						.texOffs(32, 7).addBox(-9.0F, -1.5F, -0.5F, 2.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(-4.0F, -4.5F, 0.5F, 0.0F, EAR_SWEEP, EAR_TILT));
		head.addOrReplaceChild("left_ear",
				CubeListBuilder.create()
						.texOffs(44, 0).addBox(0.0F, -1.5F, -0.5F, 4.0F, 3.0F, 1.0F)
						.texOffs(44, 4).addBox(4.0F, -1.5F, -0.5F, 3.0F, 2.0F, 1.0F)
						.texOffs(44, 7).addBox(7.0F, -1.5F, -0.5F, 2.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(4.0F, -4.5F, 0.5F, 0.0F, -EAR_SWEEP, -EAR_TILT));

		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create()
						.texOffs(0, 14).addBox(-5.0F, -3.0F, -4.5F, 10.0F, 3.0F, 9.0F)
						.texOffs(0, 26).addBox(-4.0F, 0.0F, -3.5F, 8.0F, 6.0F, 7.0F),
				PartPose.offset(0.0F, 18.0F, 0.0F));
		body.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(0, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				PartPose.offsetAndRotation(-4.5F, -2.0F, 0.0F, ARM_FORWARD, 0.0F, ARM_SPREAD));
		body.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(12, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				PartPose.offsetAndRotation(4.5F, -2.0F, 0.0F, ARM_FORWARD, 0.0F, -ARM_SPREAD));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(GroguRenderState state) {
		super.setupAnim(state);

		float age = state.ageInTicks;
		float walkPos = state.walkAnimationPos;
		float walkSpeed = Math.min(state.walkAnimationSpeed, 1.0F);

		this.head.yRot = state.yRot * DEG;
		this.head.xRot = state.xRot * DEG;

		// Ears: slow sway plus a quick twitch every few seconds.
		float twitch = (age % 90.0F) < 6.0F ? Mth.sin(age * 2.5F) * 0.15F : 0.0F;
		float sway = Mth.sin(age * 0.08F) * 0.05F;
		this.rightEar.zRot = EAR_TILT + sway + twitch;
		this.leftEar.zRot = -EAR_TILT - sway - twitch;

		// No legs under the robe: he waddles side to side with a little hop per step.
		float step = walkPos * 1.2F;
		float hop = Math.abs(Mth.sin(step)) * 0.8F * walkSpeed;
		this.body.zRot = Mth.sin(step) * 0.12F * walkSpeed;
		this.head.zRot = Mth.sin(step) * 0.08F * walkSpeed;
		this.body.y -= hop;
		this.head.y -= hop;
		this.rightArm.xRot = ARM_FORWARD + Mth.cos(step) * 0.5F * walkSpeed;
		this.leftArm.xRot = ARM_FORWARD - Mth.cos(step) * 0.5F * walkSpeed;

		// Idle: hands bob gently.
		float breathe = Mth.sin(age * 0.06F) * 0.04F;
		this.rightArm.zRot = ARM_SPREAD + breathe;
		this.leftArm.zRot = -ARM_SPREAD - breathe;

		if (state.inPouch) {
			// Riding in the pouch: still body, hands resting on the rim.
			this.body.zRot = 0.0F;
			this.head.zRot = 0.0F;
			this.rightArm.xRot = -1.25F;
			this.leftArm.xRot = -1.25F;
			this.rightArm.zRot = 0.25F;
			this.leftArm.zRot = -0.25F;
		} else if (state.sitting || state.napping) {
			this.body.y += 1.0F;
			this.head.y += 1.0F;
			this.body.zRot = 0.0F;
			this.head.zRot = 0.0F;
			this.rightArm.xRot = -0.7F;
			this.leftArm.xRot = -0.7F;
			this.rightArm.zRot = 0.15F;
			this.leftArm.zRot = -0.15F;
		}

		if (state.napping) {
			// Head droops, ears hang, slow breathing.
			this.head.xRot = 0.4F + Mth.sin(age * 0.1F) * 0.03F;
			this.head.yRot = 0.0F;
			this.rightEar.zRot = EAR_TILT - 0.5F;
			this.leftEar.zRot = -EAR_TILT + 0.5F;
			this.rightArm.xRot = -0.3F;
			this.leftArm.xRot = -0.3F;
		}

		if (state.forceAmount > 0.0F) {
			float f = state.forceAmount;
			// Concentrating: head tilts down a bit, ears pull back.
			this.head.xRot = Mth.lerp(f, this.head.xRot, 0.2F);
			this.rightEar.yRot = EAR_SWEEP + 0.3F * f;
			this.leftEar.yRot = -EAR_SWEEP - 0.3F * f;
			// Push: right hand stretched forward. Heal: both hands.
			this.rightArm.xRot = Mth.lerp(f, this.rightArm.xRot, -1.6F);
			this.rightArm.zRot = Mth.lerp(f, this.rightArm.zRot, 0.05F);

			if (state.forceType == Grogu.FORCE_HEAL) {
				this.leftArm.xRot = Mth.lerp(f, this.leftArm.xRot, -1.6F);
				this.leftArm.zRot = Mth.lerp(f, this.leftArm.zRot, -0.05F);
			}
		}
	}
}
