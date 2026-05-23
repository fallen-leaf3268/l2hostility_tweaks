package com.l2hostility_tweaks.util;

import com.l2hostility_tweaks.L2HFBypassTags;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntibuildBypassHelper {

	private static final ResourceLocation ARENA_ID = new ResourceLocation("l2hostility", "arena");

	private static final ConcurrentHashMap<UUID, Long> cache = new ConcurrentHashMap<>();
	private static final int MAX_CACHE_SIZE = 1000;

	public static boolean hasArenaTrait(Player player) {
		if (!MobTraitCap.HOLDER.isProper(player)) return false;
		var cap = MobTraitCap.HOLDER.get(player);
		for (var entry : cap.traits.entrySet()) {
			if (entry.getValue() > 0 && ARENA_ID.equals(entry.getKey().getRegistryName())) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasBypass(Player player, long gameTime) {
		UUID id = player.getUUID();
		Long cached = cache.get(id);
		if (cached != null) {
			long cachedTime = cached >> 1;
			if (cachedTime == gameTime) {
				return (cached & 1L) != 0;
			}
		}
		boolean bypass = hasArenaTrait(player) || checkBypassItem(player);
		if (cache.size() >= MAX_CACHE_SIZE) {
			cache.clear();
		}
		cache.put(id, (gameTime << 1) | (bypass ? 1L : 0L));
		return bypass;
	}

	private static boolean checkBypassItem(Player player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (player.getItemBySlot(slot).is(L2HFBypassTags.ANTIBUILD_BYPASS)) {
				return true;
			}
		}
		try {
			return CuriosApi.getCuriosInventory(player).resolve().map(handler -> {
				for (var stacksHandler : handler.getCurios().values()) {
					var stacks = stacksHandler.getStacks();
					for (int i = 0; i < stacks.getSlots(); i++) {
						if (stacks.getStackInSlot(i).is(L2HFBypassTags.ANTIBUILD_BYPASS)) {
							return true;
						}
					}
				}
				return false;
			}).orElse(false);
		} catch (Exception ignored) {
			return false;
		}
	}
}
