package com.l2hostility_tweaks.util;

public interface EntityImmunityCache {

	boolean l2fix$isImmuneToForce(long stamp);

	boolean l2fix$isImmuneToGravity(long stamp);

	ImmunityHelper.CombatCurioSnapshot l2fix$getCombatCurios(long stamp);

	void l2fix$invalidateCombatCurios();

	DimensionBreakerState l2fix$getDimensionBreakerState(long stamp);

	void l2fix$invalidateDimensionBreaker();

	record DimensionBreakerState(boolean equipped, boolean protectActive) {

		public static final DimensionBreakerState EMPTY = new DimensionBreakerState(false, false);
		public static final DimensionBreakerState EQUIPPED = new DimensionBreakerState(true, false);
		public static final DimensionBreakerState PROTECTED = new DimensionBreakerState(true, true);
	}
}
