package com.mkzv.mkzvcraft.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class GroguRenderState extends LivingEntityRenderState {
	public boolean sitting;
	public boolean napping;
	public int forceType;
	public float forceAmount;
	/** Riding in the owner's pouch: hands on the rim, no waddling. */
	public boolean inPouch;
}
