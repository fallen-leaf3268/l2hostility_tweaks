package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.DetectorGlowState;
import dev.xkmc.l2hostility.events.ClientGlowingHandler;
import dev.xkmc.l2library.util.Proxy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientGlowingHandler.class, remap = false)
public class ClientGlowingHandlerMixin {

	@Inject(method = "isGlowingImpl", at = @At("RETURN"), cancellable = true, remap = false)
	private static void l2fix$disableDetectorGlow(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;

		Player player = Proxy.getClientPlayer();
		if (player == null) return;

		if (DetectorGlowState.isGlowDisabled(player)) {
			cir.setReturnValue(false);
		}
	}
}
