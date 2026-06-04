package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.init.loot.EnvyLootModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * EnvyLootModifier 直接用 entry.getValue() 计算掉落概率，
 * 封印词条的 value 为负数，导致概率为负永不掉落。
 */
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
