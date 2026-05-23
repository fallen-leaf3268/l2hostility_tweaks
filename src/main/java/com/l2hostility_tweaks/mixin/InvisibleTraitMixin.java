package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.traits.common.InvisibleTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InvisibleTrait.class, remap = false)
public class InvisibleTraitMixin {

	@Inject(method = "postInit", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$skipShulkerEnchantForPlayer(LivingEntity mob, int lv, CallbackInfo ci) {
		if (mob instanceof Player) {
			ci.cancel();
		}
	}
}
