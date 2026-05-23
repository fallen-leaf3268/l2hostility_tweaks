package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.effect.AntiBuildEffect;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffect.class)
public class AntiBuildBeneficialMixin {

	@Inject(method = "isBeneficial", at = @At("HEAD"), cancellable = true)
	public void l2fix$makeAntiBuildBeneficial(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof AntiBuildEffect) {
			cir.setReturnValue(true);
		}
	}
}
