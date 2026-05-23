package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MobTrait.class, remap = false)
public class MobTraitDescMixin {

	private static final String UNDYING_ID = "l2hostility:undying";

	@Inject(method = "getFullDesc", at = @At("HEAD"), cancellable = true, remap = false)
	private void l2fix$exhaustedDisplay(Integer value, CallbackInfoReturnable<MutableComponent> cir) {
		if (value == null || value >= 0) return;
		LivingEntity entity = TraitDisableHelper.getDisplayEntity();
		if (entity == null) return;
		if (!UNDYING_ID.equals(((MobTrait) (Object) this).getID())) return;
		if (!TraitDisableHelper.isDisabled(entity, UNDYING_ID)) return;
		int absValue = -value;
		MutableComponent ans = Component.translatable(((MobTrait) (Object) this).getDescriptionId());
		ans = ans.append(CommonComponents.SPACE)
				.append(Component.translatable("enchantment.level." + absValue));
		ans = ans.withStyle(Style.EMPTY.withColor(((MobTrait) (Object) this).getColor()))
				.withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH);
		cir.setReturnValue(ans);
	}
}
