package com.l2hostility_tweaks.util;

import com.l2hostility_tweaks.network.NetworkHandler;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class TraitDisableHelper {

	public static final String SEAL_EXPIRY_PREFIX = "l2htweaks_seal_expiry_";
	public static final int MAX_SEAL_STATE_ENTRIES = 1024;
	public static final String UNDYING_TRAIT_ID = "l2hostility:undying";
	public static final String UNDYING_COUNT_KEY = "l2fix$undying_count";
	public static final String SEAL_STATE_MARKER = "l2htweaks_has_seal_state";
	private static final String SEALED_LEVEL_PREFIX = "l2htweaks_sealed_level_";
	private static final ThreadLocal<LivingEntity> DISPLAY_ENTITY = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> HIDE_REALITY_DETAIL = ThreadLocal.withInitial(() -> false);
	private static volatile Registry<MobTrait> traitRegistry;

	public static String sealExpiryKey(String traitId) {
		return SEAL_EXPIRY_PREFIX + traitId;
	}

	public static Map<String, Long> snapshotSealRemainingTicks(CompoundTag data, long gameTime) {
		Map<String, Long> snapshot = new LinkedHashMap<>();
		for (String key : data.getAllKeys()) {
			if (!key.startsWith(SEAL_EXPIRY_PREFIX) || !data.contains(key, Tag.TAG_LONG)) continue;
			if (snapshot.size() >= MAX_SEAL_STATE_ENTRIES) break;
			String traitId = key.substring(SEAL_EXPIRY_PREFIX.length());
			long expiry = data.getLong(key);
			long remaining = expiry <= 0 ? -1L : Math.max(0L, expiry - gameTime);
			snapshot.put(traitId, remaining);
		}
		return Map.copyOf(snapshot);
	}

	public static long sealRemainingSeconds(long initialTicks, long elapsedTicks) {
		if (initialTicks < 0) return -1L;
		long elapsed = Math.max(0L, elapsedTicks);
		if (elapsed >= initialTicks) return 0L;
		long remaining = initialTicks - elapsed;
		return remaining / 20L + (remaining % 20L == 0 ? 0L : 1L);
	}

	public static CompoundTag snapshotRuntimeState(CompoundTag data) {
		CompoundTag snapshot = new CompoundTag();
		for (String key : data.getAllKeys()) {
			if (key.startsWith(SEAL_EXPIRY_PREFIX) && data.contains(key, Tag.TAG_LONG)) {
				snapshot.putLong(key, data.getLong(key));
			}
		}
		if (data.contains(UNDYING_COUNT_KEY, Tag.TAG_INT)) {
			snapshot.putInt(UNDYING_COUNT_KEY, data.getInt(UNDYING_COUNT_KEY));
		}
		return snapshot;
	}

	public static void restoreRuntimeState(CompoundTag target, CompoundTag snapshot) {
		for (String key : snapshot.getAllKeys()) {
			if (key.startsWith(SEAL_EXPIRY_PREFIX) && snapshot.contains(key, Tag.TAG_LONG)) {
				target.putLong(key, snapshot.getLong(key));
			}
		}
		if (snapshot.contains(UNDYING_COUNT_KEY, Tag.TAG_INT)) {
			syncUndyingCountData(target, snapshot.getInt(UNDYING_COUNT_KEY));
		}
	}

	public static String sealedLevelKey(String traitId) {
		return SEALED_LEVEL_PREFIX + traitId;
	}

	public static void syncSealedLevelData(CompoundTag data, String traitId, int rawLevel) {
		String key = sealedLevelKey(traitId);
		if (rawLevel < 0) {
			data.putInt(key, Math.abs(rawLevel));
			data.putBoolean(SEAL_STATE_MARKER, true);
		} else {
			data.remove(key);
			refreshSealStateMarker(data);
		}
	}

	public static void syncUndyingCountData(CompoundTag data, int count) {
		data.putInt(UNDYING_COUNT_KEY, count);
		data.putBoolean(SEAL_STATE_MARKER, true);
	}

	public static void clearSealData(CompoundTag data, String traitId) {
		clearSealDataEntries(data, traitId);
		refreshSealStateMarker(data);
	}

	private static void clearSealDataEntries(CompoundTag data, String traitId) {
		data.remove(sealedLevelKey(traitId));
		data.remove(sealExpiryKey(traitId));
		onTraitUnsealed(data, traitId);
	}

	public static boolean hasSealStateMarker(CompoundTag data) {
		return data.getBoolean(SEAL_STATE_MARKER);
	}

	public static Set<String> reconcileSealData(CompoundTag data, Map<String, Integer> rawLevels,
			long gameTime) {
		Set<String> traitIds = new LinkedHashSet<>();
		for (Map.Entry<String, Integer> entry : rawLevels.entrySet()) {
			Integer rawLevel = entry.getValue();
			if (rawLevel != null && rawLevel < 0) {
				traitIds.add(entry.getKey());
			}
		}
		for (String key : data.getAllKeys()) {
			if (key.startsWith(SEAL_EXPIRY_PREFIX)) {
				traitIds.add(key.substring(SEAL_EXPIRY_PREFIX.length()));
			} else if (key.startsWith(SEALED_LEVEL_PREFIX)) {
				traitIds.add(key.substring(SEALED_LEVEL_PREFIX.length()));
			}
		}

		Set<String> expired = new LinkedHashSet<>();
		for (String traitId : traitIds) {
			Integer rawLevel = rawLevels.get(traitId);
			if (rawLevel == null || rawLevel >= 0) {
				clearSealDataEntries(data, traitId);
				continue;
			}

			String levelKey = sealedLevelKey(traitId);
			if (!data.contains(levelKey, Tag.TAG_INT) || data.getInt(levelKey) <= 0) {
				int restoredLevel = rawLevel == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(rawLevel);
				data.putInt(levelKey, restoredLevel);
			}

			String expiryKey = sealExpiryKey(traitId);
			if (!data.contains(expiryKey, Tag.TAG_LONG)) {
				data.putLong(expiryKey, -1L);
			} else {
				long expiry = data.getLong(expiryKey);
				if (expiry > 0 && gameTime >= expiry) {
					expired.add(traitId);
				}
			}
		}

		Integer undyingLevel = rawLevels.get(UNDYING_TRAIT_ID);
		if (data.contains(UNDYING_COUNT_KEY, Tag.TAG_INT)
				&& (undyingLevel == null || undyingLevel == 0)) {
			data.remove(UNDYING_COUNT_KEY);
		}
		refreshSealStateMarker(data);
		return Set.copyOf(expired);
	}

	public static void maintainSealState(LivingEntity entity) {
		Map<String, Integer> rawLevels = new LinkedHashMap<>();
		if (MobTraitCap.HOLDER.isProper(entity)) {
			MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
			for (Map.Entry<MobTrait, Integer> entry : cap.traits.entrySet()) {
				rawLevels.put(entry.getKey().getID(), entry.getValue());
			}
		}
		Set<String> expired = reconcileSealData(
				entity.getPersistentData(), rawLevels, entity.level().getGameTime());
		for (String traitId : expired) {
			setDisabled(entity, traitId, false);
		}
	}

	private static void refreshSealStateMarker(CompoundTag data) {
		boolean hasSealState = false;
		for (String key : data.getAllKeys()) {
			if (key.startsWith(SEAL_EXPIRY_PREFIX) || key.startsWith(SEALED_LEVEL_PREFIX)) {
				hasSealState = true;
				break;
			}
		}
		if (data.contains(UNDYING_COUNT_KEY, Tag.TAG_INT)) {
			hasSealState = true;
		}
		if (hasSealState) {
			data.putBoolean(SEAL_STATE_MARKER, true);
		} else {
			data.remove(SEAL_STATE_MARKER);
		}
	}

	public static <T> void clearTraitState(Map<T, Integer> traits, Consumer<T> reset,
			Consumer<T> clearRuntime) {
		var snapshot = new ArrayList<>(traits.keySet());
		for (T trait : snapshot) {
			reset.accept(trait);
			clearRuntime.accept(trait);
		}
		traits.clear();
	}

	public static void onTraitUnsealed(CompoundTag data, String traitId) {
		if (UNDYING_TRAIT_ID.equals(traitId)) {
			data.remove(UNDYING_COUNT_KEY);
		}
	}

	public static Component buildUndyingLimitDetail(int maxResurrections, int sealDuration) {
		if (maxResurrections < 0 || sealDuration == 0) return null;
		if (sealDuration > 0) {
			return Component.translatable("trait.l2hostility_tweaks.undying.limit_timed",
					maxResurrections, sealDuration);
		}
		return Component.translatable("trait.l2hostility_tweaks.undying.limit_permanent",
				maxResurrections);
	}

	public static boolean isUndyingLimitExhausted(int maxResurrections, int currentCount,
			int sealDuration) {
		return maxResurrections >= 0 && sealDuration != 0 && currentCount >= maxResurrections;
	}

	public static Registry<MobTrait> getTraitRegistry() {
		Registry<MobTrait> reg = traitRegistry;
		if (reg == null) {
			synchronized (TraitDisableHelper.class) {
				reg = traitRegistry;
				if (reg == null) {
					for (String key : new String[]{"l2hostility:trait", "l2hostility:mob_trait", "l2hostility:traits"}) {
						Registry<?> r = BuiltInRegistries.REGISTRY.get(new ResourceLocation(key));
						if (r != null) {
							traitRegistry = (Registry<MobTrait>) r;
							reg = traitRegistry;
							break;
						}
					}
				}
			}
		}
		return reg;
	}

	public static void setDisplayEntity(LivingEntity entity) {
		DISPLAY_ENTITY.set(entity);
	}

	public static void clearDisplayEntity() {
		DISPLAY_ENTITY.remove();
	}

	public static LivingEntity getDisplayEntity() {
		return DISPLAY_ENTITY.get();
	}

	public static void setHideRealityDetail(boolean hide) {
		HIDE_REALITY_DETAIL.set(hide);
	}

	public static boolean isHideRealityDetail() {
		return HIDE_REALITY_DETAIL.get();
	}

	public static boolean isDisabled(LivingEntity entity, String traitId) {
		return entity.getPersistentData().contains(sealedLevelKey(traitId));
	}


	public static void setDisabled(LivingEntity entity, String traitId, boolean disabled) {
		setDisabled(entity, traitId, disabled, true);
	}

	public static void setDisabled(LivingEntity entity, String traitId, boolean disabled, boolean heal) {
		if (!MobTraitCap.HOLDER.isProper(entity)) return;
		MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
		boolean isDisabled = entity.getPersistentData().contains(sealedLevelKey(traitId));
		if (disabled == isDisabled) return;
		float oldHealth = entity.getHealth();
		float oldMax = entity.getMaxHealth();
		if (disabled) {
			var opt = cap.traits.entrySet().stream().filter(e -> traitId.equals(e.getKey().getID())).findFirst();
			if (opt.isEmpty()) return;
			var entry = opt.get();
			int disabledLevel = -Math.abs(entry.getValue());
			syncSealedLevelData(entity.getPersistentData(), traitId, disabledLevel);
			entry.setValue(disabledLevel);
			entry.getKey().initialize(entity, 0);
		} else {
			int restore = entity.getPersistentData().getInt(sealedLevelKey(traitId));
			if (restore <= 0) return;
			clearSealData(entity.getPersistentData(), traitId);
			for (var e : cap.traits.entrySet()) {
				if (traitId.equals(e.getKey().getID())) {
					e.setValue(restore);
					e.getKey().initialize(entity, restore);
					e.getKey().postInit(entity, restore);
					break;
				}
			}
		}
		if (heal) {
			float ratio = oldMax > 0 ? oldHealth / oldMax : 1.0f;
			entity.setHealth(Math.max(1, entity.getMaxHealth() * ratio));
		}
		cap.syncToClient(entity);
		if (entity instanceof ServerPlayer player) {
			NetworkHandler.sendSealStateToPlayer(player);
		}
	}

}
