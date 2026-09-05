package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class DimensionBreakerMiningProtectionMixin {

	@Inject(method = "getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F", at = @At("RETURN"), cancellable = true)
	private void l2fix$limitDestroyProgress(Player player, BlockGetter level, BlockPos pos,
											CallbackInfoReturnable<Float> cir) {
		float originalProgress = cir.getReturnValue();
		float limitedProgress = l2fix$limitDestroyProgress(originalProgress,
				DimensionBreakerItem.isProtectActive(player), player.isCreative(), player.isSpectator());
		if (limitedProgress < originalProgress) {
			cir.setReturnValue(limitedProgress);
		}
	}

	private static float l2fix$limitDestroyProgress(float progress, boolean protectActive,
											boolean creative, boolean spectator) {
		return protectActive && !creative && !spectator ? Math.min(progress, 0.1F) : progress;
	}
}
