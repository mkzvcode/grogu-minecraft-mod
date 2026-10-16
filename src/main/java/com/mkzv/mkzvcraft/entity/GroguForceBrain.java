package com.mkzv.mkzvcraft.entity;

import java.util.Comparator;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Grogu's Force abilities, shared by a Grogu walking around and one riding in his owner's pouch.
 * <ul>
 * <li>Push: a hostile mob near Grogu (or near his owner) gets thrown away.</li>
 * <li>Heal: when the owner drops to half health or lower, Grogu heals them (once a minute).</li>
 * <li>Big feats tire him out: he naps after a heal or after several pushes in a row.</li>
 * </ul>
 * Each use plays a short "hand raised, eyes closed" animation; the effect fires halfway through.
 */
public final class GroguForceBrain {
	public static final int FORCE_NONE = 0;
	public static final int FORCE_PUSH = 1;
	public static final int FORCE_HEAL = 2;

	public static final int FORCE_ANIM_TICKS = 24;
	private static final int FORCE_RELEASE_TICK = FORCE_ANIM_TICKS / 2;

	private static final double PUSH_RANGE = 6.0;
	private static final double GUARD_OWNER_RANGE = 5.0;
	private static final int PUSH_COOLDOWN = 100;
	private static final int PUSHES_BEFORE_NAP = 3;
	private static final int FATIGUE_RECOVERY_TICKS = 300;
	private static final int SHORT_NAP_TICKS = 100;

	private static final double HEAL_RANGE = 10.0;
	private static final float HEAL_THRESHOLD = 0.5F;
	private static final float HEAL_AMOUNT = 8.0F;
	private static final int HEAL_COOLDOWN = 1200;
	private static final int LONG_NAP_TICKS = 200;

	/** Whoever is channelling the Force: a Grogu entity, or a player carrying one. */
	public interface Host {
		/** Where the Force comes from (Grogu's eyes). */
		Vec3 forceOrigin();

		LivingEntity owner();

		boolean isTame();

		boolean canSee(LivingEntity target);

		/** Direct entity for the push damage source. */
		Entity attacker();

		void playSound(SoundEvent sound, float volume, float pitch);

		default void onForceStart(LivingEntity target) {
		}

		default void lookAt(LivingEntity target) {
		}
	}

	private int forceTicks;
	private int forceType = FORCE_NONE;
	private int napTicks;
	private int pushCooldown;
	private int healCooldown;
	private int fatigue;
	private int fatigueRecovery;
	private LivingEntity target;

	public void tick(ServerLevel level, Host host) {
		if (this.pushCooldown > 0) {
			this.pushCooldown--;
		}

		if (this.healCooldown > 0) {
			this.healCooldown--;
		}

		if (this.fatigue > 0 && ++this.fatigueRecovery >= FATIGUE_RECOVERY_TICKS) {
			this.fatigue--;
			this.fatigueRecovery = 0;
		}

		// Finish the current Force animation first; a nap started by it begins right after.
		if (this.forceTicks > 0) {
			this.tickActiveForce(level, host);
			return;
		}

		if (this.napTicks > 0) {
			this.napTicks--;
			return;
		}

		if (level.getGameTime() % 5 != 0) {
			return;
		}

		LivingEntity owner = host.owner();
		Vec3 origin = host.forceOrigin();

		if (host.isTame() && owner instanceof Player && owner.isAlive() && this.healCooldown == 0
				&& owner.getHealth() <= owner.getMaxHealth() * HEAL_THRESHOLD
				&& owner.distanceToSqr(origin) < HEAL_RANGE * HEAL_RANGE) {
			this.start(FORCE_HEAL, owner, host);
			return;
		}

		if (this.pushCooldown == 0) {
			LivingEntity enemy = findEnemy(level, host, origin, owner);

			if (enemy != null) {
				this.start(FORCE_PUSH, enemy, host);
			}
		}
	}

	private static LivingEntity findEnemy(ServerLevel level, Host host, Vec3 origin, LivingEntity owner) {
		boolean guardOwner = host.isTame() && owner != null && owner.isAlive();
		double reach = PUSH_RANGE + GUARD_OWNER_RANGE;

		return level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(origin, reach * 2, reach * 2, reach * 2), entity ->
						entity instanceof Enemy
								&& entity.isAlive()
								&& host.canSee(entity)
								&& (entity.distanceToSqr(origin) < PUSH_RANGE * PUSH_RANGE
								|| guardOwner && entity.distanceToSqr(owner) < GUARD_OWNER_RANGE * GUARD_OWNER_RANGE))
				.stream()
				.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
				.orElse(null);
	}

	private void start(int type, LivingEntity target, Host host) {
		this.target = target;
		this.forceType = type;
		this.forceTicks = FORCE_ANIM_TICKS;
		host.onForceStart(target);
	}

	private void tickActiveForce(ServerLevel level, Host host) {
		LivingEntity target = this.target;

		if (target != null) {
			host.lookAt(target);
		}

		if (this.forceTicks == FORCE_RELEASE_TICK && target != null && target.isAlive()) {
			if (this.forceType == FORCE_HEAL) {
				this.releaseHeal(level, host, target);
			} else {
				this.releasePush(level, host, target);
			}
		}

		if (--this.forceTicks == 0) {
			this.forceType = FORCE_NONE;
			this.target = null;
		}
	}

	private void releasePush(ServerLevel level, Host host, LivingEntity target) {
		Vec3 from = host.forceOrigin();
		Vec3 to = target.position().add(0.0, target.getBbHeight() / 2.0, 0.0);

		for (int i = 1; i <= 8; i++) {
			Vec3 point = from.lerp(to, i / 8.0);
			level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 3, 0.1, 0.1, 0.1, 0.0);
		}

		level.sendParticles(ParticleTypes.GUST, to.x, to.y, to.z, 1, 0.0, 0.0, 0.0, 0.0);
		host.playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 1.3F);

		target.hurtServer(level, level.damageSources().indirectMagic(host.attacker(), host.owner()), 2.0F);

		Vec3 away = new Vec3(target.getX() - from.x, 0.0, target.getZ() - from.z).normalize();
		target.push(away.x * 1.2, 0.45, away.z * 1.2);

		this.pushCooldown = PUSH_COOLDOWN;

		if (++this.fatigue >= PUSHES_BEFORE_NAP) {
			this.fatigue = 0;
			this.napTicks = SHORT_NAP_TICKS;
		}
	}

	private void releaseHeal(ServerLevel level, Host host, LivingEntity owner) {
		owner.heal(HEAL_AMOUNT);
		owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));

		Vec3 from = host.forceOrigin();
		Vec3 to = owner.position().add(0.0, owner.getBbHeight() * 0.6, 0.0);

		for (int i = 1; i <= 10; i++) {
			Vec3 point = from.lerp(to, i / 10.0);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.0);
		}

		level.sendParticles(ParticleTypes.HEART, to.x, to.y + 0.6, to.z, 5, 0.4, 0.3, 0.4, 0.1);
		host.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 1.2F);

		this.healCooldown = HEAL_COOLDOWN;
		this.napTicks = LONG_NAP_TICKS;
	}

	public int forceTicks() {
		return this.forceTicks;
	}

	public int forceType() {
		return this.forceType;
	}

	public int napTicks() {
		return this.napTicks;
	}

	public int healCooldown() {
		return this.healCooldown;
	}

	public static boolean isNapping(int forceTicks, int napTicks) {
		return napTicks > 0 && forceTicks == 0;
	}

	/** 0 → 1 → 0 over the Force animation, peaking when the effect is released. */
	public static float forceAmount(int forceTicks, float partialTicks) {
		if (forceTicks <= 0) {
			return 0.0F;
		}

		float progress = (FORCE_ANIM_TICKS - forceTicks + partialTicks) / FORCE_ANIM_TICKS;
		return (float) Math.sin(Math.min(progress, 1.0F) * Math.PI);
	}
}
