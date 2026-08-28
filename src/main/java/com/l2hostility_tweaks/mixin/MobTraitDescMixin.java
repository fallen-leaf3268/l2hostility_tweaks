package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MobTrait.class, remap = false)
public class MobTraitDescMixin {

	@Inject(method = "addDetail", at = @At("TAIL"), remap = false)
	private void l2fix$undyingLimitDetail(List<Component> list, CallbackInfo ci) {
		MobTrait self = (MobTrait) (Object) this;
		if (!TraitDisableHelper.UNDYING_TRAIT_ID.equals(self.getID())) return;
		Component detail = TraitDisableHelper.buildUndyingLimitDetail(
				L2HConfig.getDisplayUndyingMaxResurrections(), L2HConfig.getDisplayUndyingSealDuration());
		if (detail != null) {
			list.add(detail.copy().withStyle(ChatFormatting.GOLD));
		}
	}
}
