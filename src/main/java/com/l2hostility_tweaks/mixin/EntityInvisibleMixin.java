package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.config.L2HConfig;
import dev.xkmc.l2hostility.compat.curios.CurioCompat;
import dev.xkmc.l2library.util.Proxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Entity.class)
public class EntityInvisibleMixin {

	private static final ResourceLocation DETECTOR_GLASSES_ID = new ResourceLocation("l2hostility", "detector_glasses");

	@Unique
	private static Item cachedGlasses;
	@Unique
	private static final Map<UUID, Boolean> playerGlassesCache = new ConcurrentHashMap<>();
	@Unique
	private static final Map<UUID, Integer> playerGlassesTick = new ConcurrentHashMap<>();

	@Unique
	private static Item getDetectorGlasses() {
		if (cachedGlasses == null) {
			cachedGlasses = ForgeRegistries.ITEMS.getValue(DETECTOR_GLASSES_ID);
		}
		return cachedGlasses;
	}

	@Inject(method = "isInvisibleTo", at = @At("RETURN"), cancellable = true)
	private void l2fix$revealInvisibleEntity(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		if (!L2HConfig.isDetectorGlassesRevealEnabled()) return;
		if (!((Object) this instanceof LivingEntity entity)) return;

		if (playerHasDetectorGlasses(player) && isInRevealRange(entity, player)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
	private void l2fix$revealInvisible(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		if (!L2HConfig.isDetectorGlassesRevealEnabled()) return;
		if (!((Object) this instanceof LivingEntity entity)) return;
		if (!entity.level().isClientSide()) return;

		Player player = Proxy.getClientPlayer();
		if (player == null) return;

		if (playerHasDetectorGlasses(player) && isInRevealRange(entity, player)) {
			cir.setReturnValue(false);
		}
	}

	@Unique
	private static boolean playerHasDetectorGlasses(Player player) {
		UUID id = player.getUUID();
		Integer cachedTick = playerGlassesTick.get(id);
		if (cachedTick != null && cachedTick == player.tickCount) {
			return playerGlassesCache.getOrDefault(id, false);
		}
		Item glasses = getDetectorGlasses();
		boolean has = glasses != null && CurioCompat.hasItemInCurioOrSlot(player, glasses);
		playerGlassesCache.put(id, has);
		playerGlassesTick.put(id, player.tickCount);

		if (playerGlassesCache.size() > 200) {
			var server = player.getServer();
			if (server != null) {
				var online = new java.util.HashSet<>();
				for (var p : server.getPlayerList().getPlayers()) {
					online.add(p.getUUID());
				}
				playerGlassesCache.keySet().removeIf(k -> !online.contains(k));
				playerGlassesTick.keySet().removeIf(k -> !online.contains(k));
			}
		}
		return has;
	}

	@Unique
	private static boolean isInRevealRange(LivingEntity entity, Player player) {
		int range = L2HConfig.getDetectorGlassesRange();
		double distSqr = entity.distanceToSqr(player);
		return distSqr <= (double) range * range;
	}
}
