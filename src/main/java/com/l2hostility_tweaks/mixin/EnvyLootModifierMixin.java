package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.init.loot.EnvyLootModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = EnvyLootModifier.class, remap = false)
public class EnvyLootModifierMixin {

	@Redirect(method = "doApply", at = @At(value = "INVOKE",
			target = "Ljava/util/Map$Entry;getValue()Ljava/lang/Object;",
			remap = false))
	private Object l2fix$absEnvyLevel(Map.Entry<?, ?> entry) {
		Object val = entry.getValue();
		if (val instanceof Integer i && i < 0) {
			return -i;
		}
		return val;
	}
}
