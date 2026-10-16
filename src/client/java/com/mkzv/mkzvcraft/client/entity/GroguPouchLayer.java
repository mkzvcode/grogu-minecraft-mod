package com.mkzv.mkzvcraft.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mkzv.mkzvcraft.MkzvCraft;
import com.mkzv.mkzvcraft.entity.GroguForceBrain;
import com.mkzv.mkzvcraft.pouch.GroguPouch;
import com.mkzv.mkzvcraft.pouch.PouchState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Draws the pouch and the carried Grogu on players (like a parrot on the shoulder). */
public class GroguPouchLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier POUCH_TEXTURE = MkzvCraft.id("textures/entity/grogu/pouch.png");
	private static final float GROGU_SCALE = 0.55F;
	/** Lifts Grogu so his collar peeks over the rim (pixels, body space). */
	private static final float GROGU_OFFSET_Y = -2.5F;

	private final GroguModel grogu;
	private final GroguPouchModel pouch;

	public GroguPouchLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
		super(parent);
		this.grogu = new GroguModel(models.bakeLayer(GroguModel.LAYER));
		this.pouch = new GroguPouchModel(models.bakeLayer(GroguPouchModel.LAYER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
		ClientLevel level = Minecraft.getInstance().level;

		if (level == null || state.isInvisible || !(level.getEntity(state.id) instanceof Player player)) {
			return;
		}

		PouchState animation = player.getAttached(GroguPouch.STATE);

		if (animation == null) {
			return;
		}

		GroguRenderState groguState = new GroguRenderState();
		groguState.inPouch = true;
		groguState.ageInTicks = state.ageInTicks;
		// He looks where you look, a bit lazily.
		groguState.yRot = Mth.clamp(yRot, -60.0F, 60.0F) * 0.7F;
		groguState.xRot = Mth.clamp(xRot, -30.0F, 30.0F) * 0.5F;

		groguState.napping = animation.napping();
		groguState.forceType = animation.forceType();
		groguState.forceAmount = GroguForceBrain.forceAmount(animation.forceTicks(), 0.0F);

		poseStack.pushPose();
		this.getParentModel().body.translateAndRotate(poseStack);
		collector.submitModel(this.pouch, groguState, poseStack, POUCH_TEXTURE, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

		poseStack.translate(GroguPouchModel.BAG_CENTER_X / 16.0F, GROGU_OFFSET_Y / 16.0F, GroguPouchModel.BAG_CENTER_Z / 16.0F);
		poseStack.scale(GROGU_SCALE, GROGU_SCALE, GROGU_SCALE);
		collector.submitModel(this.grogu, groguState, poseStack, GroguRenderer.textureFor(groguState), lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
