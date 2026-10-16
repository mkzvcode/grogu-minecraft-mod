package com.mkzv.grogu;

import java.util.function.Function;

import com.mkzv.grogu.entity.ModEntities;
import com.mkzv.grogu.pouch.GroguPouchItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	public static final Item GROGU_SPAWN_EGG = register("grogu_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.GROGU));
	/** Carries Grogu: click your tamed Grogu with it, click a block to let him out (see GroguPouch). */
	public static final Item GROGU_POUCH = register("grogu_pouch", GroguPouchItem::new, new Item.Properties().stacksTo(1).fireResistant());

	private ModItems() {
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, GroguMod.id(name));
		T item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(GROGU_SPAWN_EGG));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(GROGU_POUCH));
	}
}
