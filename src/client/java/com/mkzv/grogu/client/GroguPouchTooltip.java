package com.mkzv.grogu.client;

import java.util.List;

import com.mkzv.grogu.ModComponents;
import com.mkzv.grogu.ModItems;
import com.mkzv.grogu.pouch.GroguPouch;
import com.mkzv.grogu.pouch.PouchState;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Pouch tooltip: how to use it, Grogu's health and whether he is ready to heal you. */
final class GroguPouchTooltip {
	private GroguPouchTooltip() {
	}

	static void append(ItemStack stack, List<Component> lines) {
		if (!stack.is(ModItems.GROGU_POUCH)) {
			return;
		}

		CompoundTag data = stack.get(ModComponents.GROGU);

		if (data == null) {
			lines.add(Component.translatable("tooltip.grogu.grogu_pouch.empty").withStyle(ChatFormatting.GRAY));
			return;
		}

		lines.add(Component.translatable("tooltip.grogu.grogu_pouch.health",
				Math.round(GroguPouch.health(data)), Math.round(GroguPouch.GROGU_MAX_HEALTH)).withStyle(ChatFormatting.RED));

		Player player = Minecraft.getInstance().player;
		PouchState state = player == null ? null : player.getAttached(GroguPouch.STATE);

		if (state != null && GroguPouch.findWorn(player) == stack) {
			if (state.napping()) {
				lines.add(Component.translatable("tooltip.grogu.grogu_pouch.napping").withStyle(ChatFormatting.AQUA));
			} else if (state.healCooldownSeconds() > 0) {
				lines.add(Component.translatable("tooltip.grogu.grogu_pouch.heal_cooldown", state.healCooldownSeconds()).withStyle(ChatFormatting.GOLD));
			} else {
				lines.add(Component.translatable("tooltip.grogu.grogu_pouch.heal_ready").withStyle(ChatFormatting.GREEN));
			}
		} else {
			lines.add(Component.translatable("tooltip.grogu.grogu_pouch.inactive").withStyle(ChatFormatting.DARK_GRAY));
		}

		lines.add(Component.translatable("tooltip.grogu.grogu_pouch.release").withStyle(ChatFormatting.GRAY));
	}
}
