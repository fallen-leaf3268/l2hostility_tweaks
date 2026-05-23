package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.client.L2HHealthOverlay;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MobTraitCap.class, remap = false)
public class OverheadTraitMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:overhead_trait");

    @Inject(method = "getTitle", at = @At("HEAD"), cancellable = true)
    private void l2fix$hideAllTraitDisplays(boolean showLevel, boolean showTrait, CallbackInfoReturnable<List<Component>> cir) {
        LOGGER.debug("getTitle HEAD: hudActive={} showLevel={} showTrait={}", L2HHealthOverlay.hudActive, showLevel, showTrait);
        if (L2HHealthOverlay.hudActive) {
            LOGGER.debug("getTitle CANCELLED: hiding overhead traits");
            cir.setReturnValue(List.of());
        }
    }
}
