package com.mkzv.grogu.entity;

import java.util.EnumSet;

import com.mkzv.grogu.ModItems;
import com.mkzv.grogu.pouch.GroguPouch;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Small, friendly Force-sensitive creature. Wild ones live in swamps; tame one with cookies.
 * A tamed Grogu follows its owner and uses the Force (see {@link GroguForceBrain}).
 * The owner can carry him around in a pouch (see {@link GroguPouch}).
 */
public class Grogu extends TamableAnimal implements GroguForceBrain.Host {
	private static final EntityDataAccessor<Integer> DATA_FORCE_TICKS = SynchedEntityData.defineId(Grogu.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FORCE_TYPE = SynchedEntityData.defineId(Grogu.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_NAP_TICKS = SynchedEntityData.defineId(Grogu.class, EntityDataSerializers.INT);

	public static final int FORCE_NONE = GroguForceBrain.FORCE_NONE;
	public static final int FORCE_PUSH = GroguForceBrain.FORCE_PUSH;
	public static final int FORCE_HEAL = GroguForceBrain.FORCE_HEAL;

	private final GroguForceBrain brain = new GroguForceBrain();

	public Grogu(EntityType<? extends Grogu> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(1, new NapGoal());
		this.goalSelector.addGoal(2, new TamableAnimal.TamableAnimalPanicGoal(1.4));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, Grogu::isTreat, false));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2, 6.0F, 2.0F));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_FORCE_TICKS, 0);
		entityData.define(DATA_FORCE_TYPE, FORCE_NONE);
		entityData.define(DATA_NAP_TICKS, 0);
	}

	private static boolean isTreat(ItemStack stack) {
		return stack.is(Items.COOKIE);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return isTreat(stack) || stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH);
	}

	@Override
	public boolean canFallInLove() {
		return false;
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (stack.is(ModItems.GROGU_POUCH)) {
			return GroguPouch.putIn(this, player, stack);
		}

		if (this.isTame()) {
			if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
				this.feed(player, hand, stack, 2.0F, 4.0F);
				return InteractionResult.SUCCESS;
			}

			if (this.isOwnedBy(player)) {
				this.setOrderedToSit(!this.isOrderedToSit());
				this.jumping = false;
				this.navigation.stop();
				return InteractionResult.SUCCESS.withoutItem();
			}

			return super.mobInteract(player, hand);
		}

		if (isTreat(stack)) {
			if (!this.level().isClientSide()) {
				stack.consume(1, player);
				this.tryToTame(player);
			}

			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	private void tryToTame(Player player) {
		if (this.random.nextInt(3) == 0) {
			this.tame(player);
			this.navigation.stop();
			this.setOrderedToSit(true);
			this.level().broadcastEntityEvent(this, (byte) 7);
		} else {
			this.level().broadcastEntityEvent(this, (byte) 6);
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level() instanceof ServerLevel serverLevel) {
			this.brain.tick(serverLevel, this);
			this.syncForceState();
		}
	}

	private void syncForceState() {
		if (this.entityData.get(DATA_FORCE_TICKS) != this.brain.forceTicks()) {
			this.entityData.set(DATA_FORCE_TICKS, this.brain.forceTicks());
		}

		if (this.entityData.get(DATA_FORCE_TYPE) != this.brain.forceType()) {
			this.entityData.set(DATA_FORCE_TYPE, this.brain.forceType());
		}

		if (this.entityData.get(DATA_NAP_TICKS) != this.brain.napTicks()) {
			this.entityData.set(DATA_NAP_TICKS, this.brain.napTicks());
		}
	}

	// --- GroguForceBrain.Host ---

	@Override
	public Vec3 forceOrigin() {
		return this.getEyePosition();
	}

	@Override
	public LivingEntity owner() {
		return this.getOwner();
	}

	@Override
	public boolean canSee(LivingEntity target) {
		return this.hasLineOfSight(target);
	}

	@Override
	public Entity attacker() {
		return this;
	}

	@Override
	public void onForceStart(LivingEntity target) {
		this.navigation.stop();
	}

	@Override
	public void lookAt(LivingEntity target) {
		this.getLookControl().setLookAt(target, 30.0F, 30.0F);
	}

	// --- Client-side state for rendering ---

	public boolean isNapping() {
		return GroguForceBrain.isNapping(this.entityData.get(DATA_FORCE_TICKS), this.entityData.get(DATA_NAP_TICKS));
	}

	public int getForceType() {
		return this.entityData.get(DATA_FORCE_TYPE);
	}

	public float getForceAmount(float partialTicks) {
		return GroguForceBrain.forceAmount(this.entityData.get(DATA_FORCE_TICKS), partialTicks);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isNapping() ? null : SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.8F + (this.random.nextFloat() - 0.5F) * 0.2F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	/** Keeps Grogu still while he naps: blocks movement and looking goals. */
	private class NapGoal extends Goal {
		NapGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return Grogu.this.isNapping();
		}

		@Override
		public void start() {
			Grogu.this.navigation.stop();
		}
	}
}
