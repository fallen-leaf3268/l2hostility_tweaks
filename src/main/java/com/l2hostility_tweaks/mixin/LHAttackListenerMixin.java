package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.events.LHAttackListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 封印词条（value < 0）不参与 modifyBonusDamage 伤害加成。
 * 将负数等级钳位到 0，modifyBonusDamage(0) = 1（无加成）。
 */
@Mixin(value = LHAttackListener.class, remap = false)
public class LHAttackListenerMixin {

	@ModifyArg(
			method = "onHurt",
			at = @At(value = "INVOKE",
					target = "Ldev/xkmc/l2hostility/content/traits/base/MobTrait;modifyBonusDamage(Lnet/minecraft/world/damagesource/DamageSource;DI)D"),
			index = 2)
	private int l2fix$clampSealedLevel(int level) {
		return Math.max(0, level);
	}
}
