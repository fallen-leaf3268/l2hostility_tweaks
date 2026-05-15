package com.l2hostilityfix.mixin;

import com.l2hostilityfix.client.L2HHealthOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeGui.class, remap = false)
public class BossBarMixin {

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void l2fix$hideBossBars(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (L2HHealthOverlay.hideBossBars) {
            ci.cancel();
        }
    }
}
