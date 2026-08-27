package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import com.l2hostility_tweaks.util.DetectorGlassesCache;
import dev.xkmc.l2library.util.Proxy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityInvisibleMixin {

	@Inject(method = "isInvisibleTo", at = @At("RETURN"), cancellable = true)
	private void l2fix$revealInvisibleEntity(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		if (!((Object) this instanceof LivingEntity entity)) return;
		boolean revealEnabled = entity.level().isClientSide()
				? L2HConfig.isDisplayDetectorGlassesRevealEnabled()
				: L2HConfig.isDetectorGlassesRevealEnabled();
		if (!revealEnabled) return;

		if (l2fix$playerHasDetectorGlasses(player) && l2fix$isInRevealRange(entity, player)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
	private void l2fix$revealInvisible(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		if (!((Object) this instanceof LivingEntity entity)) return;
		if (!entity.level().isClientSide()) return;
		if (!L2HConfig.isDisplayDetectorGlassesRevealEnabled()) return;

		Player player = Proxy.getClientPlayer();
		if (player == null) return;

		if (l2fix$playerHasDetectorGlasses(player) && l2fix$isInRevealRange(entity, player)) {
			cir.setReturnValue(false);
		}
	}

	@Unique
	private static boolean l2fix$playerHasDetectorGlasses(Player player) {
		return ((DetectorGlassesCache) player).l2fix$hasDetectorGlasses();
	}

	@Unique
	private static boolean l2fix$isInRevealRange(LivingEntity entity, Player player) {
		int range = entity.level().isClientSide()
				? L2HConfig.getDisplayDetectorGlassesRange()
				: L2HConfig.getDetectorGlassesRange();
		double distSqr = entity.distanceToSqr(player);
		return distSqr <= (double) range * range;
	}
}
