package com.l2hostility_tweaks.util;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class TraitDisableHelper {

	public static final String SEAL_EXPIRY_PREFIX = "l2htweaks_seal_expiry_";
	private static final ThreadLocal<LivingEntity> DISPLAY_ENTITY = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> HIDE_REALITY_DETAIL = ThreadLocal.withInitial(() -> false);
	private static Registry<dev.xkmc.l2hostility.content.traits.base.MobTrait> traitRegistry;
	public static String sealExpiryKey(String traitId) {
		return SEAL_EXPIRY_PREFIX + traitId;
	}

	public static Registry<dev.xkmc.l2hostility.content.traits.base.MobTrait> getTraitRegistry() {
		if (traitRegistry == null) {
			for (String key : new String[]{"l2hostility:trait", "l2hostility:mob_trait", "l2hostility:traits"}) {
				Registry<?> reg = BuiltInRegistries.REGISTRY.get(new ResourceLocation(key));
				if (reg != null) {
					traitRegistry = (Registry<dev.xkmc.l2hostility.content.traits.base.MobTrait>) reg;
					break;
				}
			}
		}
		return traitRegistry;
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
		if (!MobTraitCap.HOLDER.isProper(entity)) return false;
		MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
		for (var entry : cap.traits.entrySet()) {
			if (traitId.equals(entry.getKey().getID())) {
				return entry.getValue() < 0;
			}
		}
		return false;
	}

	public static void setDisabled(LivingEntity entity, String traitId, boolean disabled) {
		setDisabled(entity, traitId, disabled, true);
	}

	public static void setDisabled(LivingEntity entity, String traitId, boolean disabled, boolean heal) {
		if (!MobTraitCap.HOLDER.isProper(entity)) return;
		MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
		for (var entry : cap.traits.entrySet()) {
			if (traitId.equals(entry.getKey().getID())) {
				int level = entry.getValue();
				int absLevel = Math.abs(level);
				boolean isDisabled = level < 0;
				if (disabled == isDisabled) return;
				var trait = entry.getKey();
				if (disabled) {
					trait.initialize(entity, 0);
					entry.setValue(-absLevel);
				} else {
					entry.setValue(absLevel);
					trait.initialize(entity, absLevel);
					trait.postInit(entity, absLevel);
				}
				if (heal) {
					entity.setHealth(entity.getMaxHealth());
				}
				cap.syncToClient(entity);
				return;
			}
		}
	}
}
