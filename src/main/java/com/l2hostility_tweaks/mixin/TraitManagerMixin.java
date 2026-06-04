package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.content.logic.TraitManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截 TraitManager.fill()，当 disable_mob_level 开启时返回 0，
 * 阻止生物获得难度等级、血量缩放和词条生成。
 */
@Mixin(value = TraitManager.class, remap = false)
public class TraitManagerMixin {

	@Inject(method = "fill", at = @At("HEAD"), cancellable = true)
	private static void l2fix$disableMobLevel(CallbackInfoReturnable<Integer> cir) {
		if (L2HConfig.isDisableMobLevel()) {
			cir.setReturnValue(0);
		}
	}
}
