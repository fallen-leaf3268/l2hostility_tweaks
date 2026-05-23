package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.content.DimensionBreakerItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Item.class)
public class DimensionBreakerHarvestMixin {

	@Inject(method = "getHarvestLevel", at = @At("RETURN"), cancellable = true, remap = false)
	private void l2fix$breakerHarvestLevel(ItemStack stack, ToolAction tool, @Nullable Player player, BlockState state, CallbackInfoReturnable<Integer> cir) {
		if (player == null || !DimensionBreakerItem.isEquippedBy(player)) return;
		if (tool == ToolActions.PICKAXE_DIG || tool == ToolActions.AXE_DIG || tool == ToolActions.SHOVEL_DIG) {
			cir.setReturnValue(Integer.MAX_VALUE);
		}
	}
}
