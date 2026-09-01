package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import com.l2hostility_tweaks.util.RomanNumeral;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MobTrait.class, remap = false)
public class TraitRomanMixin {

    @Inject(method = "getFullDesc", at = @At("HEAD"), cancellable = true, remap = false)
    private void l2fix$formatLevel(Integer value, CallbackInfoReturnable<MutableComponent> cir) {
        if (value == null) return;
        MobTrait self = (MobTrait)(Object)this;
        MutableComponent ans = self.getDesc().copy();
        int level = Math.abs(value);
        if (level > 1) {
            String levelText = l2fix$levelText(value, ClientL2HConfig.CLIENT.romanNumerals.get());
            ans.append(CommonComponents.SPACE).append(Component.literal(levelText));
        }
        ans = value < 0
                ? ans.withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH)
                : ans.withStyle(Style.EMPTY.withColor(self.getColor()));
        cir.setReturnValue(ans);
    }

    @Unique
    private static String l2fix$levelText(int value, boolean romanNumerals) {
        int level = Math.abs(value);
        if (level <= 1) return null;
        return romanNumerals ? RomanNumeral.toRoman(level) : String.valueOf(level);
    }
}
