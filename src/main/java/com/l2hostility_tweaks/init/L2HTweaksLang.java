package com.l2hostility_tweaks.init;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class L2HTweaksLang {

	// === 玩家自我词条 ===
	public static final String SELF_TRAIT_MAX_COUNT = "message.l2hostility_tweaks.self_trait_max_count";
	public static final String SELF_TRAIT_BLACKLISTED = "message.l2hostility_tweaks.self_trait_blacklisted";
	public static final String SELF_TRAIT_MIN_LEVEL = "message.l2hostility_tweaks.self_trait_min_level";
	public static final String SELF_TRAIT_BUDGET_EXCEEDED = "message.l2hostility_tweaks.self_trait_budget_exceeded";
	public static final String SELF_TRAIT_MUTUAL_EXCLUSION = "message.l2hostility_tweaks.self_trait_mutual_exclusion";
	public static final String SELF_TRAIT_COST_INFO = "message.l2hostility_tweaks.self_trait_cost_info";
	public static final String SELF_TRAIT_ADDED = "message.l2hostility_tweaks.self_trait_added";
	public static final String SELF_TRAIT_NOT_ENOUGH_ITEMS = "message.l2hostility_tweaks.self_trait_not_enough_items";

	// === 生物词条限制 ===
	public static final String MOB_TRAIT_MIN_LEVEL = "message.l2hostility_tweaks.mob_trait_min_level";
	public static final String MOB_TRAIT_BUDGET_EXCEEDED = "message.l2hostility_tweaks.mob_trait_budget_exceeded";
	public static final String MOB_TRAIT_COST_INFO = "message.l2hostility_tweaks.mob_trait_cost_info";

	// === 难度界面 ===
	public static final String LEVEL_CAP_UNLIMITED = "info.l2hostility_tweaks.level_cap_unlimited";
	public static final String LEVEL_CAP = "info.l2hostility_tweaks.level_cap";
	public static final String LEGENDARY_UNLIMITED = "info.l2hostility_tweaks.legendary_unlimited";
	public static final String LEGENDARY_PRESET = "info.l2hostility_tweaks.legendary_preset";
	public static final String LEGENDARY_COUNT = "info.l2hostility_tweaks.legendary_count";

	// === 扭曲之魂 ===
	public static final String ABRAHADABRA_MINION_TOOLTIP = "tooltip.l2hostility_tweaks.abrahadabra_minion";

	// === 词条卸载工具 ===
	public static final String UNLOADER_TOOLTIP = "tooltip.l2hostility_tweaks.trait_unloader";
	public static final String UNLOADER_MODE = "tooltip.l2hostility_tweaks.unloader_mode";
	public static final String UNLOADER_MODE_CHANGED = "message.l2hostility_tweaks.unloader_mode_changed";
	public static final String UNLOADER_MODE_SINGLE = "mode.l2hostility_tweaks.single";
	public static final String UNLOADER_MODE_GROUP = "mode.l2hostility_tweaks.group";
	public static final String UNLOADER_MODE_FULL = "mode.l2hostility_tweaks.full";
	public static final String UNLOADER_NO_TRAITS = "message.l2hostility_tweaks.unloader_no_traits";
	public static final String UNLOADER_NO_SELECTED_TRAIT = "message.l2hostility_tweaks.unloader_no_selected_trait";
	public static final String UNLOADER_SINGLE = "message.l2hostility_tweaks.unloader_single";
	public static final String UNLOADER_GROUP = "message.l2hostility_tweaks.unloader_group";
	public static final String UNLOADER_FULL = "message.l2hostility_tweaks.unloader_full";

	public static final String SEAL_TOOLTIP = "tooltip.l2hostility_tweaks.trait_seal";
	public static final String SEAL_CURRENT = "tooltip.l2hostility_tweaks.trait_seal.current";
	public static final String SEAL_SELECTED = "message.l2hostility_tweaks.trait_seal.selected";
	public static final String SEAL_SEALED = "message.l2hostility_tweaks.trait_seal.sealed";
	public static final String SEAL_UNSEALED = "message.l2hostility_tweaks.trait_seal.unsealed";
	public static final String SEAL_NO_TRAIT = "message.l2hostility_tweaks.trait_seal.no_trait";
	public static final String SEAL_NOT_A_MOB = "message.l2hostility_tweaks.trait_seal.not_a_mob";

	public static final String UNLOAD_HINT = "gui.l2hostility_tweaks.unload_hint";
	public static final String UNLOAD_ALL_HINT = "gui.l2hostility_tweaks.unload_all_hint";
	public static final String UPGRADE_COST = "gui.l2hostility_tweaks.upgrade_cost";

	public static MutableComponent translate(String key, Object... args) {
		return Component.translatable(key, args);
	}
}
