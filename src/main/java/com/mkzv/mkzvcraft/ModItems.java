package com.mkzv.mkzvcraft;

import java.util.function.Function;

import com.mkzv.mkzvcraft.entity.ModEntities;
import com.mkzv.mkzvcraft.pouch.GroguPouchItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	public static final Item TEST_GEM = register("test_gem", Item::new, new Item.Properties().stacksTo(16));
	public static final Item GROGU_SPAWN_EGG = register("grogu_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.GROGU));
	/** Carries Grogu: click your tamed Grogu with it, click a block to let him out (see GroguPouch). */
	public static final Item GROGU_POUCH = register("grogu_pouch", GroguPouchItem::new, new Item.Properties().stacksTo(1).fireResistant());

	private ModItems() {
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MkzvCraft.id(name));
		T item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(TEST_GEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(GROGU_SPAWN_EGG));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(GROGU_POUCH));
	}
}
