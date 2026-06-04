package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import com.l2hostility_tweaks.util.RomanNumeral;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 拦截 MobTraitCap.getTitle() 中对 getFullDesc 的调用。
 * 封印词条（value < 0）：灰色+删除线。
 * 非封印词条：保留原本颜色和等级格式。
 * 不调用原 getFullDesc 以避免递归。
 */
@Mixin(value = MobTraitCap.class, remap = false)
public class GetTitleMixin {

	@Redirect(
			method = "getTitle",
			at = @At(value = "INVOKE",
					target = "Ldev/xkmc/l2hostility/content/traits/base/MobTrait;getFullDesc(Ljava/lang/Integer;)Lnet/minecraft/network/chat/MutableComponent;"),
			remap = false
	)
	private MutableComponent l2fix$sealedInTitle(MobTrait trait, Integer value) {
		int lv = value != null ? Math.abs(value) : 0;
		MutableComponent text = Component.literal("").append(trait.getDesc().copy());
		if (lv > 1) {
			String levelText = ClientL2HConfig.CLIENT.romanNumerals.get()
					? RomanNumeral.toRoman(lv)
					: String.valueOf(lv);
			text.append(CommonComponents.SPACE).append(Component.literal(levelText));
		}
		if (value != null && value < 0) {
			text = text.withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH);
		} else {
			text = text.withStyle(Style.EMPTY.withColor(trait.getColor()));
		}
		return Component.literal("").withStyle(Style.EMPTY).append(text);
	}
}
