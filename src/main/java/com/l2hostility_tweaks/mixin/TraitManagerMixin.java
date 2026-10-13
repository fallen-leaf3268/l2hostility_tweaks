package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.content.logic.TraitManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TraitManager.class, remap = false)
public class TraitManagerMixin {

	@Inject(method = "fill", at = @At("HEAD"), cancellable = true)
	private static void l2fix$disableMobLevel(CallbackInfoReturnable<Integer> cir) {
		if (L2HConfig.isDisableMobLevel()) {
			cir.setReturnValue(0);
		}
	}
}
