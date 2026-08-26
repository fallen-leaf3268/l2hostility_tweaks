package com.l2hostility_tweaks.util;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class TraitDisableHelper {

	public static final String SEAL_EXPIRY_PREFIX = "l2htweaks_seal_expiry_";
	public static final String UNDYING_TRAIT_ID = "l2hostility:undying";
	public static final String UNDYING_COUNT_KEY = "l2fix$undying_count";
	private static final String SEALED_LEVEL_PREFIX = "l2htweaks_sealed_level_";
	private static final ThreadLocal<LivingEntity> DISPLAY_ENTITY = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> HIDE_REALITY_DETAIL = ThreadLocal.withInitial(() -> false);
	private static volatile Registry<MobTrait> traitRegistry;

	public static String sealExpiryKey(String traitId) {
		return SEAL_EXPIRY_PREFIX + traitId;
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
			target.putInt(UNDYING_COUNT_KEY, snapshot.getInt(UNDYING_COUNT_KEY));
		}
	}

	private static String sealedLevelKey(String traitId) {
		return SEALED_LEVEL_PREFIX + traitId;
	}

	public static void syncSealedLevelData(CompoundTag data, String traitId, int rawLevel) {
		String key = sealedLevelKey(traitId);
		if (rawLevel < 0) {
			data.putInt(key, Math.abs(rawLevel));
		} else {
			data.remove(key);
		}
	}

	public static void clearSealData(CompoundTag data, String traitId) {
		data.remove(sealedLevelKey(traitId));
		data.remove(sealExpiryKey(traitId));
		onTraitUnsealed(data, traitId);
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
			entity.getPersistentData().putInt(sealedLevelKey(traitId), Math.abs(entry.getValue()));
			entry.setValue(-Math.abs(entry.getValue()));
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
	}

}
