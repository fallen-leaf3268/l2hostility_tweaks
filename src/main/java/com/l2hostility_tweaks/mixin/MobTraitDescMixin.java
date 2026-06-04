package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
}
