package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.TraitDisableHelper;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.events.ClientEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderNameTagEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientEvents.class, remap = false)
public class ClientEventsMixin {

	@Inject(method = "renderNamePlate", at = @At("HEAD"), remap = false)
	private static void l2fix$captureEntity(RenderNameTagEvent event, CallbackInfo ci) {
		if (event.getEntity() instanceof LivingEntity le && MobTraitCap.HOLDER.isProper(le)) {
			TraitDisableHelper.setDisplayEntity(le);
		}
	}

	@Inject(method = "renderNamePlate", at = @At("TAIL"), remap = false)
	private static void l2fix$clearEntity(RenderNameTagEvent event, CallbackInfo ci) {
		TraitDisableHelper.clearDisplayEntity();
	}
}
