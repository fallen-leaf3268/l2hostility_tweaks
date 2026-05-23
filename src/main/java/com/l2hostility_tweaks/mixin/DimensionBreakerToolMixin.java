package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class DimensionBreakerToolMixin {

	@Inject(method = "hasCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
	private void l2fix$dimensionBreakerTool(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && DimensionBreakerItem.canHarvest((Player) (Object) this, state)) {
			cir.setReturnValue(true);
		}
	}
}
