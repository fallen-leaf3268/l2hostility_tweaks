package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.client.PlayerTraitScreen;
import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.events.ClientEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientEvents.class, remap = false)
public class RenderNamePlateMixin {

    @Inject(method = "renderNamePlate", at = @At("HEAD"), cancellable = true)
    private static void l2fix$cancelOverheadTrait(RenderNameTagEvent event, CallbackInfo ci) {
        if (L2HConfig.COMMON.showHud.get() && !(event.getEntity() instanceof Player)) {
            ci.cancel();
            return;
        }
        if (event.getEntity() instanceof Player && !PlayerTraitScreen.playerOverheadEnabled) {
            ci.cancel();
        }
    }
}
