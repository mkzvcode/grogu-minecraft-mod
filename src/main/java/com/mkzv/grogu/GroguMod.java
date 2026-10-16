package com.mkzv.grogu;

import com.mkzv.grogu.entity.ModEntities;
import com.mkzv.grogu.pouch.GroguPouch;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroguMod implements ModInitializer {
	public static final String MOD_ID = "grogu";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModComponents.init();
		ModEntities.init();
		ModItems.init();
		GroguPouch.init();

		LOGGER.info("Grogu loaded!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
