package com.mkzv.grogu;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModComponents {
	/** Saved Grogu inside a pouch item. Its presence also switches the pouch icon to "with Grogu". */
	public static final DataComponentType<CompoundTag> GROGU = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, GroguMod.id("grogu"),
			DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());

	private ModComponents() {
	}

	public static void init() {
		// Loads the class so the components are registered.
	}
}
