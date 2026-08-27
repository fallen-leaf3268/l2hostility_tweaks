package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MobTrait.class, remap = false)
public class MobTraitDescMixin {

	@Inject(method = "getFullDesc", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$sealedDisplay(Integer value, CallbackInfoReturnable<MutableComponent> cir) {
		if (value == null || value >= 0) return;
		MutableComponent ans = Component.translatable(((MobTrait) (Object) this).getDescriptionId());
		ans = ans.append(CommonComponents.SPACE)
				.append(Component.translatable("enchantment.level." + (-value)));
		ans = ans.withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH);
		cir.setReturnValue(ans);
	}

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
