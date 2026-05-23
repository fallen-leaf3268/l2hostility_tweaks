package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.entity.HostilityBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HostilityBullet.class, remap = false)
public class HostilityBulletMixin {

	@Inject(method = "isTarget", at = @At("RETURN"), cancellable = true)
	private void l2fix$adjustTargeting(Entity e, CallbackInfoReturnable<Boolean> cir) {
		Entity owner = ((HostilityBullet) (Object) this).getOwner();
		if (owner == null) return;

		if (cir.getReturnValue()) {
			if (e == owner) {
				cir.setReturnValue(false);
				return;
			}
			if (e instanceof net.minecraft.world.entity.OwnableEntity ownable) {
				var ownerUuid = ownable.getOwnerUUID();
				if (ownerUuid != null && ownerUuid.equals(owner.getUUID())) {
					cir.setReturnValue(false);
				}
			}
			return;
		}

		if (owner instanceof net.minecraft.world.entity.player.Player
				&& e instanceof LivingEntity
				&& e != owner
				&& !e.isAlliedTo(owner)) {
			cir.setReturnValue(true);
		}
	}
}
