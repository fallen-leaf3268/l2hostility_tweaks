package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.AntibuildBypassHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ItemStack.class)
public class AntibuildPlaceBypassMixin {

	private static final ResourceLocation ANTIBUILD_ID = new ResourceLocation("l2hostility", "antibuild");
	@Unique
	private static final ConcurrentHashMap<UUID, MobEffectInstance> l2fix$pendingRestore = new ConcurrentHashMap<>();

	@Unique
	private static MobEffect l2fix$cachedEffect;

	@Unique
	private static MobEffect getAntibuild() {
		if (l2fix$cachedEffect == null) {
			l2fix$cachedEffect = ForgeRegistries.MOB_EFFECTS.getValue(ANTIBUILD_ID);
		}
		return l2fix$cachedEffect;
	}

	@Inject(method = "useOn", at = @At("HEAD"))
	public void l2fix$bypassAntibuildPlace(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		Player player = ctx.getPlayer();
		if (player == null) return;
		MobEffect antibuild = getAntibuild();
		if (antibuild == null || !player.hasEffect(antibuild)) return;
		if (!AntibuildBypassHelper.hasBypass(player, player.level().getGameTime())) return;

		MobEffectInstance stale = l2fix$pendingRestore.remove(player.getUUID());
		if (stale != null && !player.hasEffect(antibuild)) {
			player.addEffect(stale);
		}

		MobEffectInstance instance = player.getEffect(antibuild);
		if (instance != null) {
			l2fix$pendingRestore.put(player.getUUID(), new MobEffectInstance(instance));
			player.removeEffect(antibuild);
		}
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	public void l2fix$restoreAntibuild(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		Player player = ctx.getPlayer();
		if (player == null) return;
		MobEffectInstance pending = l2fix$pendingRestore.remove(player.getUUID());
		if (pending != null) {
			player.addEffect(pending);
		}
	}
}
