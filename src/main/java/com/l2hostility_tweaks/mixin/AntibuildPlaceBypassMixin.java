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
	private static final long PENDING_TIMEOUT_TICKS = 100;
	private static final int MAX_PENDING_SIZE = 200;

	@Unique
	private static final ConcurrentHashMap<UUID, MobEffectInstance> l2fix$pendingRestore = new ConcurrentHashMap<>();
	@Unique
	private static final ConcurrentHashMap<UUID, Long> l2fix$pendingTime = new ConcurrentHashMap<>();

	@Unique
	private static MobEffect l2fix$cachedEffect;

	@Unique
	private static MobEffect l2fix$getAntibuild() {
		if (l2fix$cachedEffect == null) {
			l2fix$cachedEffect = ForgeRegistries.MOB_EFFECTS.getValue(ANTIBUILD_ID);
		}
		return l2fix$cachedEffect;
	}

	@Inject(method = "useOn", at = @At("HEAD"))
	public void l2fix$bypassAntibuildPlace(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		Player player = ctx.getPlayer();
		if (player == null) return;
		MobEffect antibuild = l2fix$getAntibuild();
		if (antibuild == null || !player.hasEffect(antibuild)) return;
		if (!AntibuildBypassHelper.hasBypass(player, player.level().getGameTime())) return;

		MobEffectInstance stale = l2fix$pendingRestore.remove(player.getUUID());
		l2fix$pendingTime.remove(player.getUUID());
		if (stale != null && !player.hasEffect(antibuild)) {
			player.addEffect(stale);
		}

		if (l2fix$pendingRestore.size() > MAX_PENDING_SIZE) {
			l2fix$pendingRestore.clear();
			l2fix$pendingTime.clear();
		}

		MobEffectInstance instance = player.getEffect(antibuild);
		if (instance != null) {
			l2fix$pendingRestore.put(player.getUUID(), new MobEffectInstance(instance));
			l2fix$pendingTime.put(player.getUUID(), (long) player.tickCount);
			player.removeEffect(antibuild);
		}
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	public void l2fix$restoreAntibuild(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		Player player = ctx.getPlayer();
		if (player == null) return;
		Long ts = l2fix$pendingTime.remove(player.getUUID());
		MobEffectInstance pending = l2fix$pendingRestore.remove(player.getUUID());
		if (pending != null && ts != null && player.tickCount - ts <= PENDING_TIMEOUT_TICKS) {
			player.addEffect(pending);
		}
	}
}
