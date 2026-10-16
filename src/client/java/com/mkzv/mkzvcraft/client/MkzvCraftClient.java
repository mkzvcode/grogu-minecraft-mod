package com.mkzv.mkzvcraft.client;

import com.mkzv.mkzvcraft.client.entity.GroguModel;
import com.mkzv.mkzvcraft.client.entity.GroguPouchLayer;
import com.mkzv.mkzvcraft.client.entity.GroguPouchModel;
import com.mkzv.mkzvcraft.client.entity.GroguRenderer;
import com.mkzv.mkzvcraft.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class MkzvCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(GroguModel.LAYER, GroguModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(GroguPouchModel.LAYER, GroguPouchModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.GROGU, GroguRenderer::new);

		ItemTooltipCallback.EVENT.register((stack, tooltipContext, flag, lines) -> GroguPouchTooltip.append(stack, lines));

		// Grogu riding in the pouch is drawn on the player model.
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new GroguPouchLayer(avatar, context.getModelSet()));
			}
		});
	}
}
