package com.l2hostility_tweaks.content;

import com.l2hostility_tweaks.util.ImmunityHelper;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.AttackListener;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import net.minecraft.world.item.ItemStack;

public class RingDamageListener implements AttackListener {

	@Override
	public void onHurt(AttackCache cache, ItemStack weapon) {
		var attacker = cache.getAttacker();
		if (attacker == null) return;

		for (float multiplier : ImmunityHelper.getCombatCurios(attacker).ringMultipliers()) {
			cache.addHurtModifier(DamageModifier.multTotal(multiplier));
		}
	}
}
