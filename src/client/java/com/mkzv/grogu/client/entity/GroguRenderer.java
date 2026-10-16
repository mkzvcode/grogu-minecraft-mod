package com.mkzv.grogu.client.entity;

import com.mkzv.grogu.GroguMod;
import com.mkzv.grogu.entity.Grogu;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class GroguRenderer extends MobRenderer<Grogu, GroguRenderState, GroguModel> {
	private static final Identifier TEXTURE = GroguMod.id("textures/entity/grogu/grogu.png");
	/** Eyes closed: used while napping and while concentrating on the Force. */
	private static final Identifier TEXTURE_EYES_CLOSED = GroguMod.id("textures/entity/grogu/grogu_eyes_closed.png");

	public GroguRenderer(EntityRendererProvider.Context context) {
		super(context, new GroguModel(context.bakeLayer(GroguModel.LAYER)), 0.35F);
	}

	@Override
	public GroguRenderState createRenderState() {
		return new GroguRenderState();
	}

	@Override
	public void extractRenderState(Grogu entity, GroguRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.sitting = entity.isInSittingPose();
		state.napping = entity.isNapping();
		state.forceType = entity.getForceType();
		state.forceAmount = entity.getForceAmount(partialTicks);
	}

	@Override
	public Identifier getTextureLocation(GroguRenderState state) {
		return textureFor(state);
	}

	public static Identifier textureFor(GroguRenderState state) {
		return state.napping || state.forceAmount > 0.3F ? TEXTURE_EYES_CLOSED : TEXTURE;
	}
}
