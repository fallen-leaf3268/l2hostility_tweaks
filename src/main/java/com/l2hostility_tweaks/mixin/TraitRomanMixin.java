package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.client.config.ClientL2HConfig;
import com.l2hostility_tweaks.util.RomanNumeral;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
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
        if (value > 1) {
            String levelText = ClientL2HConfig.CLIENT.romanNumerals.get()
                    ? RomanNumeral.toRoman(value)
                    : String.valueOf(value);
            ans.append(CommonComponents.SPACE).append(Component.literal(levelText));
        }
        ans = ans.withStyle(Style.EMPTY.withColor(self.getColor()));
        cir.setReturnValue(ans);
    }
}
