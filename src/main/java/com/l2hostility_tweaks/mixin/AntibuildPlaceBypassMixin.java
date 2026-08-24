package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.AntibuildBypassHelper;
import dev.xkmc.l2hostility.events.MiscHandlers;
import dev.xkmc.l2hostility.init.registrate.LHEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MiscHandlers.class, remap = false)
public class AntibuildPlaceBypassMixin {

	@Inject(method = "useOnSkip", at = @At("HEAD"), cancellable = true)
	private static void l2fix$bypassAntibuildPlace(UseOnContext ctx, ItemStack stack,
			CallbackInfoReturnable<Boolean> cir) {
		Player player = ctx.getPlayer();
		if (player == null) return;
		boolean hasAntibuild = player.hasEffect(LHEffects.ANTIBUILD.get());
		if (!hasAntibuild) return;
		boolean hasBypass = AntibuildBypassHelper.hasBypass(player, player.level().getGameTime());
		if (l2fix$shouldBypass(hasAntibuild, hasBypass)) {
			cir.setReturnValue(false);
		}
	}

	@Unique
	static boolean l2fix$shouldBypass(boolean hasAntibuild, boolean hasBypass) {
		return hasAntibuild && hasBypass;
	}
}
