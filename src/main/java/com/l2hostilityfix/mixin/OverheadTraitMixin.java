package com.l2hostilityfix.mixin;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MobTraitCap.class, remap = false)
public class OverheadTraitMixin {

    @Inject(method = "getTitle", at = @At("HEAD"), cancellable = true)
    private void l2fix$hideAllTraitDisplays(boolean showLevel, boolean showTrait, CallbackInfoReturnable<List<Component>> cir) {
        cir.setReturnValue(List.of());
    }
}
