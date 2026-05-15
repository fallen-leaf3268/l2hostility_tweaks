package com.l2hostilityfix.mixin;

import com.l2hostilityfix.L2HFBypassTags;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.traits.legendary.DispellTrait;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DispellTrait.class, remap = false)
public class DispellTraitMixin {

	@Inject(method = "onDamaged", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$bypassDispell(int level, LivingEntity entity, AttackCache cache, CallbackInfo ci) {
		var event = cache.getLivingDamageEvent();
		if (event == null) return;
		if (event.getSource().is(L2HFBypassTags.BYPASSES_DISPELL)) {
			ci.cancel();
		}
	}
}
