package com.mkzv.mkzvcraft.entity;

import com.mkzv.mkzvcraft.MkzvCraft;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> GROGU_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MkzvCraft.id("grogu"));
	public static final EntityType<Grogu> GROGU = Registry.register(BuiltInRegistries.ENTITY_TYPE, GROGU_KEY,
			EntityType.Builder.of(Grogu::new, MobCategory.CREATURE)
					.sized(0.55F, 0.95F)
					.eyeHeight(0.7F)
					.clientTrackingRange(10)
					.build(GROGU_KEY));

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(GROGU, Grogu.createAttributes());
		SpawnPlacements.register(GROGU, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
		// Rare: a lone Grogu in swamps (Dagobah vibes).
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.SWAMP, Biomes.MANGROVE_SWAMP), MobCategory.CREATURE, GROGU, 3, 1, 1);
	}
}
