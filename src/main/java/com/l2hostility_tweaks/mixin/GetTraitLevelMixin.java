package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让 getTraitLevel() 对封印词条返回 0（等级为 0，不触发任何效果）。
 * hasTrait() 单独处理：封印词条仍然"存在"（hasTrait 返回 true）。
 * 内部仍用负值在 traits map 中传递封印状态（同步到客户端供显示层识别）。
 */
@Mixin(value = MobTraitCap.class, remap = false)
public class GetTraitLevelMixin {

	@Inject(method = "getTraitLevel", at = @At("RETURN"), cancellable = true, remap = false)
	private void l2fix$sealedZero(MobTrait trait, CallbackInfoReturnable<Integer> cir) {
		if (cir.getReturnValue() < 0) cir.setReturnValue(0);
	}

	@Inject(method = "hasTrait", at = @At("RETURN"), cancellable = true, remap = false)
	private void l2fix$sealedExists(MobTrait trait, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) return;
		// 直接读 traits 原始值（绕过 getTraitLevel mixin）
		if (((MobTraitCap) (Object) this).traits.getOrDefault(trait, 0) != 0) cir.setReturnValue(true);
	}
}
