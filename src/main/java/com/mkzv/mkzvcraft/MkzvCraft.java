package com.mkzv.mkzvcraft;

import com.mkzv.mkzvcraft.entity.ModEntities;
import com.mkzv.mkzvcraft.pouch.GroguPouch;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MkzvCraft implements ModInitializer {
	public static final String MOD_ID = "mkzvcraft";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModComponents.init();
		ModEntities.init();
		ModItems.init();
		GroguPouch.init();

		LOGGER.info("MKZV Craft loaded!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
