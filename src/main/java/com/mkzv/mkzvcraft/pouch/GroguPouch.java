package com.mkzv.mkzvcraft.pouch;

import java.util.ArrayList;
import java.util.List;

import com.mkzv.mkzvcraft.MkzvCraft;
import com.mkzv.mkzvcraft.ModComponents;
import com.mkzv.mkzvcraft.ModItems;
import com.mkzv.mkzvcraft.entity.Grogu;
import com.mkzv.mkzvcraft.entity.GroguForceBrain;
import com.mkzv.mkzvcraft.entity.ModEntities;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Carrying Grogu in a pouch. The pouch item holds the whole Grogu (owner, health, name...).
 * <ul>
 * <li>Right-click your tamed Grogu with an empty pouch: he climbs in and the icon changes.</li>
 * <li>While the pouch is in your inventory he rides on your body (drawn on the player) and keeps
 * using the Force: pushes enemies, heals you, naps. He also slowly regains health in there.</li>
 * <li>Right-click a block with the pouch: he climbs out.</li>
 * <li>If the pouch ends up on the ground (thrown, or you died) he jumps out by himself.</li>
 * </ul>
 */
public final class GroguPouch {
	/** Present while a player carries Grogu: what he is doing, for rendering and the tooltip. */
	public static final AttachmentType<PouchState> STATE = AttachmentRegistry.create(MkzvCraft.id("grogu_pouch_state"),
			builder -> builder.syncWith(PouchState.STREAM_CODEC, AttachmentSyncPredicate.all()));
	/** Server-only Force state (cooldowns, fatigue) of the carried Grogu. */
	private static final AttachmentType<GroguForceBrain> BRAIN = AttachmentRegistry.create(MkzvCraft.id("grogu_pouch_brain"));
	/** Old versions kept Grogu on the player instead of in the item; moved into a pouch on load. */
	private static final AttachmentType<CompoundTag> LEGACY_CARRIED = AttachmentRegistry.create(MkzvCraft.id("carried_grogu"),
			builder -> builder.persistent(CompoundTag.CODEC));

	public static final float GROGU_MAX_HEALTH = 12.0F;
	private static final int POUCH_REGEN_INTERVAL = 600;

	private static final List<ItemEntity> droppedPouches = new ArrayList<>();

	private GroguPouch() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(GroguPouch::tick);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && GroguPouchItem.hasGrogu(item.getItem())) {
				// Handled at the end of the tick: don't spawn entities while one is being added.
				droppedPouches.add(item);
			}
		});
	}

	/** First pouch with Grogu in the player's inventory: that one is "worn". */
	public static ItemStack findWorn(Player player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);

			if (stack.is(ModItems.GROGU_POUCH) && GroguPouchItem.hasGrogu(stack)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}

	public static boolean isCarrying(Player player) {
		return player.hasAttached(STATE);
	}

	/** Owner right-clicked Grogu with a pouch. Changes the pouch in place, so creative can't duplicate anything. */
	public static InteractionResult putIn(Grogu grogu, Player player, ItemStack pouch) {
		if (!grogu.isTame() || !grogu.isOwnedBy(player)) {
			return fail(player, "message.mkzvcraft.grogu_pouch.not_yours");
		}

		if (GroguPouchItem.hasGrogu(pouch)) {
			return fail(player, "message.mkzvcraft.grogu_pouch.occupied");
		}

		if (player instanceof ServerPlayer) {
			grogu.dropLeash();
			pouch.set(ModComponents.GROGU, save(grogu));
			pouch.set(DataComponents.ITEM_NAME, Component.translatable("item.mkzvcraft.grogu_pouch.full"));
			grogu.playSound(SoundEvents.BUNDLE_INSERT, 1.0F, 1.0F);
			grogu.discard();
		}

		return InteractionResult.SUCCESS;
	}

	private static InteractionResult fail(Player player, String messageKey) {
		if (player instanceof ServerPlayer) {
			player.sendOverlayMessage(Component.translatable(messageKey));
		}

		return InteractionResult.FAIL;
	}

	/** Lets Grogu out of {@code pouch} at {@code pos}; the pouch becomes empty (in place). */
	public static Grogu letOut(ServerLevel level, ItemStack pouch, BlockPos pos, float yaw) {
		CompoundTag data = pouch.get(ModComponents.GROGU);
		pouch.remove(ModComponents.GROGU);
		pouch.set(DataComponents.ITEM_NAME, ModItems.GROGU_POUCH.components().get(DataComponents.ITEM_NAME));

		if (data == null) {
			return null;
		}

		Grogu grogu = ModEntities.GROGU.create(level, EntitySpawnReason.BUCKET);

		if (grogu == null) {
			return null;
		}

		TypedEntityData.<EntityType<?>>of(ModEntities.GROGU, data).loadInto(grogu);
		grogu.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		grogu.setYHeadRot(yaw);
		grogu.setYBodyRot(yaw);
		// Out of the pouch he is ready to follow, not sitting.
		grogu.setOrderedToSit(false);
		grogu.setInSittingPose(false);
		level.addFreshEntity(grogu);
		grogu.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 1.0F, 1.0F);
		return grogu;
	}

	public static float health(CompoundTag data) {
		return data.getFloat("Health").orElse(GROGU_MAX_HEALTH);
	}

	private static CompoundTag save(Grogu grogu) {
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(grogu.problemPath(), MkzvCraft.LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(reporter, grogu.registryAccess());
			grogu.saveWithoutId(output);
			CompoundTag tag = output.buildResult();
			tag.remove("UUID");
			tag.remove("Motion");
			return tag;
		}
	}

	private static void tick(MinecraftServer server) {
		for (ItemEntity item : droppedPouches) {
			if (item.isAlive() && item.level() instanceof ServerLevel level && GroguPouchItem.hasGrogu(item.getItem())) {
				ItemStack pouch = item.getItem().copy();
				letOut(level, pouch, item.blockPosition(), item.getYRot());
				item.setItem(pouch);
			}
		}

		droppedPouches.clear();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			migrateLegacy(player);
			tickCarrier(player);
		}
	}

	private static void tickCarrier(ServerPlayer player) {
		ItemStack pouch = player.isAlive() && !player.isSpectator() ? findWorn(player) : ItemStack.EMPTY;

		if (pouch.isEmpty()) {
			if (player.hasAttached(STATE)) {
				player.removeAttached(STATE);
				player.removeAttached(BRAIN);
			}

			return;
		}

		GroguForceBrain brain = player.getAttachedOrCreate(BRAIN, GroguForceBrain::new);
		brain.tick(player.level(), new CarrierHost(player));

		// Resting in the pouch slowly heals him.
		if (player.level().getGameTime() % POUCH_REGEN_INTERVAL == 0) {
			CompoundTag data = pouch.get(ModComponents.GROGU);

			if (data != null && health(data) < GROGU_MAX_HEALTH) {
				CompoundTag healed = data.copy();
				healed.putFloat("Health", Math.min(GROGU_MAX_HEALTH, health(data) + 1.0F));
				pouch.set(ModComponents.GROGU, healed);
			}
		}

		PouchState state = new PouchState(brain.forceTicks(), brain.forceType(),
				GroguForceBrain.isNapping(brain.forceTicks(), brain.napTicks()),
				(brain.healCooldown() + 19) / 20);

		if (!state.equals(player.getAttached(STATE))) {
			player.setAttached(STATE, state);
		}
	}

	private static void migrateLegacy(ServerPlayer player) {
		CompoundTag legacy = player.removeAttached(LEGACY_CARRIED);

		if (legacy != null) {
			ItemStack pouch = new ItemStack(ModItems.GROGU_POUCH);
			pouch.set(ModComponents.GROGU, legacy);
			pouch.set(DataComponents.ITEM_NAME, Component.translatable("item.mkzvcraft.grogu_pouch.full"));

			if (!player.getInventory().add(pouch)) {
				player.spawnAtLocation(player.level(), pouch);
			}
		}
	}

	/** The carried Grogu channels the Force from the pouch on his owner's belly. */
	private record CarrierHost(ServerPlayer player) implements GroguForceBrain.Host {
		@Override
		public Vec3 forceOrigin() {
			float yaw = this.player.yBodyRot * ((float) Math.PI / 180.0F);
			Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
			return this.player.position().add(0.0, 1.05, 0.0).add(forward.scale(0.35));
		}

		@Override
		public LivingEntity owner() {
			return this.player;
		}

		@Override
		public boolean isTame() {
			return true;
		}

		@Override
		public boolean canSee(LivingEntity target) {
			return this.player.hasLineOfSight(target);
		}

		@Override
		public Entity attacker() {
			return this.player;
		}

		@Override
		public void playSound(SoundEvent sound, float volume, float pitch) {
			this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
		}
	}
}
