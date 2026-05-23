package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.compat.kubejs.SpellDamageFlags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public class SpellBypassResistanceMixin {

	@Inject(method = "is", at = @At("HEAD"), cancellable = true)
	private void l2fix$checkBypassFlags(TagKey<DamageType> tag, CallbackInfoReturnable<Boolean> cir) {
		if (SpellDamageFlags.isBypassEnabled(tag)) {
			cir.setReturnValue(true);
		}
	}
}
