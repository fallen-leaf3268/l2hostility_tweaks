package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.EntityImmunityCache;
import com.l2hostility_tweaks.util.ImmunityHelper;
import com.l2hostility_tweaks.content.DimensionBreakerItem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public class LivingEntityImmunityCacheMixin implements EntityImmunityCache {

	@Unique
	private volatile long l2fix$forceImmunityStamp = Long.MIN_VALUE;
	@Unique
	private boolean l2fix$forceImmunity;
	@Unique
	private volatile long l2fix$gravityImmunityStamp = Long.MIN_VALUE;
	@Unique
	private boolean l2fix$gravityImmunity;
	@Unique
	private volatile long l2fix$combatCurioStamp = Long.MIN_VALUE;
	@Unique
	private ImmunityHelper.CombatCurioSnapshot l2fix$combatCurios =
			ImmunityHelper.CombatCurioSnapshot.EMPTY;
	@Unique
	private volatile long l2fix$dimensionBreakerStamp = Long.MIN_VALUE;
	@Unique
	private EntityImmunityCache.DimensionBreakerState l2fix$dimensionBreakerState =
			EntityImmunityCache.DimensionBreakerState.EMPTY;

	@Override
	public boolean l2fix$isImmuneToForce(long stamp) {
		if (l2fix$forceImmunityStamp != stamp) {
			l2fix$forceImmunity = l2fix$scanForceImmunity();
			l2fix$forceImmunityStamp = stamp;
		}
		return l2fix$forceImmunity;
	}

	@Override
	public boolean l2fix$isImmuneToGravity(long stamp) {
		if (l2fix$gravityImmunityStamp != stamp) {
			l2fix$gravityImmunity = l2fix$scanGravityImmunity();
			l2fix$gravityImmunityStamp = stamp;
		}
		return l2fix$gravityImmunity;
	}

	@Override
	public ImmunityHelper.CombatCurioSnapshot l2fix$getCombatCurios(long stamp) {
		if (l2fix$combatCurioStamp != stamp) {
			l2fix$combatCurios = l2fix$scanCombatCurios();
			l2fix$combatCurioStamp = stamp;
		}
		return l2fix$combatCurios;
	}

	@Override
	public void l2fix$invalidateCombatCurios() {
		l2fix$combatCurioStamp = Long.MIN_VALUE;
	}

	@Override
	public EntityImmunityCache.DimensionBreakerState l2fix$getDimensionBreakerState(long stamp) {
		if (l2fix$dimensionBreakerStamp != stamp) {
			l2fix$dimensionBreakerState = l2fix$scanDimensionBreakerState();
			l2fix$dimensionBreakerStamp = stamp;
		}
		return l2fix$dimensionBreakerState;
	}

	@Override
	public void l2fix$invalidateDimensionBreaker() {
		l2fix$dimensionBreakerStamp = Long.MIN_VALUE;
	}

	@Unique
	boolean l2fix$scanForceImmunity() {
		return ImmunityHelper.computeImmuneToForce((LivingEntity) (Object) this);
	}

	@Unique
	boolean l2fix$scanGravityImmunity() {
		return ImmunityHelper.computeImmuneToGravity((LivingEntity) (Object) this);
	}

	@Unique
	ImmunityHelper.CombatCurioSnapshot l2fix$scanCombatCurios() {
		return ImmunityHelper.computeCombatCurios((LivingEntity) (Object) this);
	}

	@Unique
	EntityImmunityCache.DimensionBreakerState l2fix$scanDimensionBreakerState() {
		return DimensionBreakerItem.computeEquippedState((LivingEntity) (Object) this);
	}
}
