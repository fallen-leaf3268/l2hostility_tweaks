package com.l2hostility_tweaks.content;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.AttackListener;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public class RingDamageListener implements AttackListener {

	@Override
	public void onHurt(AttackCache cache, ItemStack weapon) {
		var attacker = cache.getAttacker();
		if (attacker == null) return;

		CuriosApi.getCuriosInventory(attacker).ifPresent(handler -> {
			for (var stacksHandler : handler.getCurios().values()) {
				var stacks = stacksHandler.getStacks();
				for (int i = 0; i < stacks.getSlots(); i++) {
					ItemStack stack = stacks.getStackInSlot(i);
					if (stack.getItem() instanceof RingItem ring) {
						cache.addHurtModifier(DamageModifier.multTotal(ring.getDamageMultiplier()));
					}
				}
			}
		});
	}
}
