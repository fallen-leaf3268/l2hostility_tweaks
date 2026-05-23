package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2library.capability.entity.GeneralCapabilityHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GeneralCapabilityHolder.class, remap = false)
public class GeneralCapabilityHolderMixin {

	@Shadow
	@Final
	public ResourceLocation id;

	@Inject(method = "shouldHaveCap", at = @At("HEAD"), cancellable = true, remap = false)
	public void l2fix$allowPlayerTraits(ICapabilityProvider entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Player && id.equals(new ResourceLocation("l2hostility", "traits"))) {
			cir.setReturnValue(true);
		}
	}
}
