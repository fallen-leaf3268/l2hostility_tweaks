package com.l2hostility_tweaks.mixin;

import dev.xkmc.l2hostility.content.entity.HostilityBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HostilityBullet.class, remap = false)
public class HostilityBulletMixin {

	@Inject(method = "isTarget", at = @At("RETURN"), cancellable = true)
	private void l2fix$adjustTargeting(Entity e, CallbackInfoReturnable<Boolean> cir) {
		Entity owner = ((HostilityBullet) (Object) this).getOwner();
		if (owner == null) return;
		if (owner instanceof Player) {
			Player ownerPlayer = (Player) owner;
			boolean ownedByOwner = e instanceof OwnableEntity ownable
					&& owner.getUUID().equals(ownable.getOwnerUUID());
			boolean canHarmCandidate = !(e instanceof Player candidate)
					|| ownerPlayer.canHarmPlayer(candidate);
			cir.setReturnValue(l2fix$isValidPlayerOwnedTarget(
					e instanceof LivingEntity && e.isAlive(),
					e == owner,
					EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e) && canHarmCandidate,
					e.isAlliedTo(owner),
					owner.isAlliedTo(e),
					ownedByOwner));
			return;
		}

		if (cir.getReturnValue()) {
			if (e == owner) {
				cir.setReturnValue(false);
				return;
			}
			if (e instanceof OwnableEntity ownable) {
				var ownerUuid = ownable.getOwnerUUID();
				if (ownerUuid != null && ownerUuid.equals(owner.getUUID())) {
					cir.setReturnValue(false);
				}
			}
			return;
		}
	}

	@Unique
	private static boolean l2fix$isValidPlayerOwnedTarget(boolean livingAndAlive,
			boolean self, boolean vanillaTargetable, boolean candidateAlliedToOwner,
			boolean ownerAlliedToCandidate, boolean ownedByOwner) {
		return livingAndAlive && !self && vanillaTargetable
				&& !candidateAlliedToOwner && !ownerAlliedToCandidate && !ownedByOwner;
	}
}
