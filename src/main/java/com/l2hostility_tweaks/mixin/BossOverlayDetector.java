package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.client.L2HHealthOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossOverlayDetector {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void l2fix$detectBossEvents(GuiGraphics guiGraphics, CallbackInfo ci) {
        L2HHealthOverlay.bossEventsActive = !((BossHealthOverlayAccessor) this).getEvents().isEmpty();
        if (L2HHealthOverlay.hideBossBars) {
            ci.cancel();
        }
    }
}
