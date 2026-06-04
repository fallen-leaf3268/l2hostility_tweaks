package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.loot.TraitLootModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * TraitLootModifier 中有两处依赖 hasTrait/getTraitLevel，
 * 封印词条的 value 为负数，hasTrait 返回 false 且 getTraitLevel 返回负值，
 * 导致封印词条无法掉落。
 */
@Mixin(value = TraitLootModifier.class, remap = false)
public class TraitLootModifierMixin {

	@Redirect(method = "doApply", at = @At(value = "INVOKE",
			target = "Ldev/xkmc/l2hostility/content/capability/mob/MobTraitCap;hasTrait(Ldev/xkmc/l2hostility/content/traits/base/MobTrait;)Z",
			remap = false))
	private boolean l2fix$hasTraitAllowSealed(MobTraitCap cap, MobTrait trait) {
		return cap.traits.getOrDefault(trait, 0) != 0;
	}

	@Redirect(method = "doApply", at = @At(value = "INVOKE",
			target = "Ldev/xkmc/l2hostility/content/capability/mob/MobTraitCap;getTraitLevel(Ldev/xkmc/l2hostility/content/traits/base/MobTrait;)I",
			remap = false))
	private int l2fix$absTraitLevel(MobTraitCap cap, MobTrait trait) {
		return Math.abs(cap.traits.getOrDefault(trait, 0));
	}
}
