package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.content.TranquilBeltItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerHurtMixin {

	@Inject(method = "hurtTo", at = @At("HEAD"), cancellable = true)
	private void l2fix$cancelHurtTo(float yRot, CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (TranquilBeltItem.isWearing(self)) {
			ci.cancel();
		}
	}
}
