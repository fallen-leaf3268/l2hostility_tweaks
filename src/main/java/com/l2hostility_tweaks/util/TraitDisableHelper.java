package com.l2hostility_tweaks.util;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

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
	}

	public static void onTraitUnsealed(CompoundTag data, String traitId) {
		if (UNDYING_TRAIT_ID.equals(traitId)) {
			data.remove(UNDYING_COUNT_KEY);
		}
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
			entity.getPersistentData().remove(sealedLevelKey(traitId));
			entity.getPersistentData().remove(sealExpiryKey(traitId));
			for (var e : cap.traits.entrySet()) {
				if (traitId.equals(e.getKey().getID())) {
					e.setValue(restore);
					e.getKey().initialize(entity, restore);
					e.getKey().postInit(entity, restore);
					onTraitUnsealed(entity.getPersistentData(), traitId);
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
