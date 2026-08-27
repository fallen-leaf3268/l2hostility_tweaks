package com.l2hostility_tweaks.util;

public interface EntityImmunityCache {

	boolean l2fix$isImmuneToForce(long stamp);

	boolean l2fix$isImmuneToGravity(long stamp);

	ImmunityHelper.CombatCurioSnapshot l2fix$getCombatCurios(long stamp);

	void l2fix$invalidateCombatCurios();
}
