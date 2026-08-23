package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MobTraitCap.class, remap = false)
public class GetTraitLevelMixin {

	@Inject(method = "getTraitLevel", at = @At("RETURN"), cancellable = true, remap = false)
	private void l2fix$sealedZero(MobTrait trait, CallbackInfoReturnable<Integer> cir) {
		if (!l2fix$isActiveLevel(cir.getReturnValue())) cir.setReturnValue(0);
	}

	@Inject(method = "hasTrait", at = @At("RETURN"), cancellable = true, remap = false)
	private void l2fix$sealedInactive(MobTrait trait, CallbackInfoReturnable<Boolean> cir) {
		int rawLevel = ((MobTraitCap) (Object) this).traits.getOrDefault(trait, 0);
		if (!l2fix$isActiveLevel(rawLevel)) cir.setReturnValue(false);
	}

	static boolean l2fix$isActiveLevel(int rawLevel) {
		return rawLevel > 0;
	}
}
