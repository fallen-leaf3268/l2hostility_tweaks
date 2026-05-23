package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class DimensionBreakerItemStackMixin {

	@Inject(method = "isCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
	private void l2fix$breakerCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) return;
		Player player = Minecraft.getInstance().player;
		if (player != null && DimensionBreakerItem.canHarvest(player, state)) {
			cir.setReturnValue(true);
		}
	}
}
