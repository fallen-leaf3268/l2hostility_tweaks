package com.l2hostility_tweaks.config;

import com.l2hostility_tweaks.util.TraitCostHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class L2HConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:config");

    public static final int MAX_DISPLAY_CONFIG_ENTRIES = 4096;
    public static final int MAX_DISPLAY_CONFIG_STRING_LENGTH = 21_845;
    public static final int MAX_DISPLAY_CONFIG_TOTAL_STRING_LENGTH = 262_144;

    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    private static volatile List<int[]> parsedLevelThresholds;
    private static volatile Map<String, int[]> parsedPerTraitThresholds;
    private static volatile List<int[]> parsedLegendaryThresholds;
    private static volatile Set<String> parsedExtraLegendaryIds;
    private static volatile List<ExclusionGroup> parsedExclusionGroups;
    private static volatile List<Integer> parsedSealDurationArray;
    private static volatile Map<String, PlayerTraitOverride> parsedPlayerTraitOverrides;
    private static volatile DisplaySnapshot displaySnapshot;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "l2_configs/l2hostility_tweaks.toml");
    }

    public static class Common {

        // === 复印 ===
        public final ForgeConfigSpec.BooleanValue reprintLinearEnabled;
        public final ForgeConfigSpec.DoubleValue reprintDamageFactor;
        public final ForgeConfigSpec.DoubleValue antiReprintReduction;

        // === 适应 ===
        public final ForgeConfigSpec.BooleanValue adaptiveLinearEnabled;
        public final ForgeConfigSpec.DoubleValue adaptiveReductionPerStack;
        public final ForgeConfigSpec.DoubleValue adaptiveMaxReduction;

        // === 探测目镜 ===
        public final ForgeConfigSpec.BooleanValue detectorGlassesReveal;
        public final ForgeConfigSpec.IntValue detectorGlassesRange;

        public final ForgeConfigSpec.DoubleValue walkingBootsMovementSpeedCap;

        // === 旧版防御 ===
        public final ForgeConfigSpec.BooleanValue oldDispell;
        public final ForgeConfigSpec.BooleanValue oldDementor;

        // === 不死 ===
        public final ForgeConfigSpec.IntValue undyingMaxResurrections;
        public final ForgeConfigSpec.IntValue undyingSealDuration;

        // === 等级限制 ===
        public final ForgeConfigSpec.BooleanValue levelCapEnabled;
        public final ForgeConfigSpec.IntValue levelCapUnlimited;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> levelCapThresholds;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> levelCapPerTrait;

        // === 封印词条 ===
        public final ForgeConfigSpec.IntValue sealDurationMode;
        public final ForgeConfigSpec.IntValue sealDurationLinear;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> sealDurationArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> ragnarokCountArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> ragnarokTimeArray;

		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> killerAuraDamageArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> killerAuraIntervalArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> dispellTimeArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> drainDamageArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> drainDurationArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> drainDurationMaxArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> drainCountArray;
		public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> dispellCountArray;
        // === 传奇限制 ===
        public final ForgeConfigSpec.BooleanValue legendaryEnabled;
        public final ForgeConfigSpec.BooleanValue legendaryBypassVanillaGate;
        public final ForgeConfigSpec.IntValue legendaryUnlimited;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> legendaryThresholds;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> extraLegendaryIds;

        // === 词条互斥 ===
        public final ForgeConfigSpec.BooleanValue exclusionEnabled;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> exclusionGroups;

        // === 词条生成 ===
        public final ForgeConfigSpec.BooleanValue disableNonPresetTraits;
        public final ForgeConfigSpec.BooleanValue disableAllTraits;
        public final ForgeConfigSpec.BooleanValue disableMobLevel;

        // === 玩家词条 ===
        public final ForgeConfigSpec.IntValue playerMaxTraits;
        public final ForgeConfigSpec.BooleanValue playerSelfTraitEnabled;
        public final ForgeConfigSpec.BooleanValue playerSelfTraitBalanceEnabled;
        public final ForgeConfigSpec.DoubleValue playerSelfTraitBudgetRatio;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> playerTraitOverrides;
        public final ForgeConfigSpec.IntValue playerSelfTraitCostMode;
        public final ForgeConfigSpec.BooleanValue playerTraitLimitEnabled;
        public final ForgeConfigSpec.DoubleValue playerTraitBudgetRatio;

        Common(ForgeConfigSpec.Builder builder) {

            builder.push("reprint");
            reprintLinearEnabled = builder.comment("启用 复印 词条线性伤害")
                    .define("linear_enabled", false);
            reprintDamageFactor = builder.comment("每级附魔等级的增伤比例")
                    .defineInRange("damage_factor", 0.05, 0.0, 1.0);
            antiReprintReduction = builder.comment("复印抵抗附魔每级减伤比例")
                    .defineInRange("counter_reduction", 0.02, 0.0, 1.0);
            builder.pop();

            builder.push("adaptive");
            adaptiveLinearEnabled = builder.comment("启用 适应 词条线性减伤")
                    .define("enabled", true);
            adaptiveReductionPerStack = builder.comment("每层适应的减伤比例")
                    .defineInRange("reduction_per_stack", 0.25, 0.0, 1.0);
            adaptiveMaxReduction = builder.comment("适应的最大减伤上限")
                    .defineInRange("max_reduction", 0.95, 0.0, 1.0);
            builder.pop();

            builder.push("detector_glasses");
            detectorGlassesReveal = builder.comment("佩戴探测目镜时直接显示隐身生物")
                    .define("reveal_invisible", true);
            detectorGlassesRange = builder.comment("探测目镜显示隐身生物的范围（格）")
                    .defineInRange("reveal_range", 48, 1, 256);
            builder.pop();

            builder.push("walking_boots");
            walkingBootsMovementSpeedCap = builder.comment("漫步之靴限制的玩家最终移动速度上限")
                    .defineInRange("movement_speed_cap", 0.15, 0.0, 1024.0);
            builder.pop();

            builder.push("legendary_defense");
            oldDispell = builder.comment("启用破魔词条旧版免疫魔法伤害")
                    .define("old_dispell", false);
            oldDementor = builder.comment("启用摄魂词条旧版免疫非魔法伤害")
                    .define("old_dementor", false);
            builder.pop();

            builder.push("seal_trait");
            sealDurationMode = builder.comment("封印词条持续时间模式",
                    "1 = 线性: duration = level x duration_linear",
                    "2 = 数组: 每级取自 duration_array，超出后线性补齐")
                    .defineInRange("duration_mode", 1, 1, 2);
            sealDurationLinear = builder.comment("线性模式每级封印时间")
                    .defineInRange("duration_linear", 3, 1, 3600);
            sealDurationArray = builder.comment("数组模式每级对应的封印时间")
                    .defineList("duration_array", List.of(), L2HConfig::isPositiveInteger);
            builder.pop();

            builder.push("ragnarok");
            ragnarokCountArray = builder.comment("诸神黄昏数组模式配置",
                    "每级封印物品数量")
                    .defineList("count_array", List.of(), L2HConfig::isNonNegativeInteger);
            ragnarokTimeArray = builder.comment("每级封印时长 (tick)")
                    .defineList("time_array", List.of(), L2HConfig::isPositiveInteger);
            builder.pop();

            builder.push("killer_aura");
            killerAuraDamageArray = builder.comment("Killer Aura 数组配置",
                    "每级伤害")
                    .defineList("damage_array", List.of(), L2HConfig::isPositiveInteger);
            killerAuraIntervalArray = builder.comment("每级攻击间隔 (tick)，必须大于 0")
                    .defineList("interval_array", List.of(), L2HConfig::isPositiveInteger);
            builder.pop();

            builder.push("dispell");
            dispellTimeArray = builder.comment("Dispell 数组配置",
                    "每级封印时长 (tick)")
                    .defineList("time_array", List.of(), L2HConfig::isPositiveInteger);
            dispellCountArray = builder.comment("每级封印物品数量")
                    .defineList("count_array", List.of(), L2HConfig::isNonNegativeInteger);
            builder.pop();

            builder.push("drain");
            drainDamageArray = builder.comment("Drain 数组配置",
                    "每级伤害加成")
                    .defineList("damage_array", List.of(), L2HConfig::isNonNegativeInteger);
            drainDurationArray = builder.comment("每级时长时间")
                    .defineList("duration_array", List.of(), L2HConfig::isNonNegativeInteger);
            drainDurationMaxArray = builder.comment("每级最高延长时间 (s)")
                    .defineList("duration_max_array", List.of(), L2HConfig::isNonNegativeInteger);
            drainCountArray = builder.comment("每级剥夺效果数量")
                    .defineList("count_array", List.of(), L2HConfig::isNonNegativeInteger);
            builder.pop();

            builder.push("undying");
            undyingMaxResurrections = builder.comment("不死词条最大重生次数，-1 无限制")
                    .defineInRange("max_resurrections", -1, -1, 114514);
            undyingSealDuration = builder.comment("不死词条耗尽后封印时长（秒），-1 永久，0 不封印")
                    .defineInRange("seal_duration", 0, -1, 3600);
            builder.pop();

            builder.push("level_cap");
            levelCapEnabled = builder.comment("启用词条等级阶梯限制")
                    .define("enabled", false);
            levelCapUnlimited = builder.comment("超过此难度取消等级上限")
                    .defineInRange("unlimited_threshold", 1000, 0, Integer.MAX_VALUE);
            levelCapThresholds = builder.comment("全局难度阶梯限制",
                    "例: \"200,2\" = 难度 >= 200 时最高等级为 2")
                    .defineList("thresholds", List.of(),
                            e -> e instanceof String s && s.matches("\\d+,\\d+"));
            levelCapPerTrait = builder.comment("独立难度阶梯限制",
                    "例: \"l2hostility:repelling,100,200\" = lv2需难度100, lv3需难度200")
                    .defineList("per_trait", List.of(),
                            e -> e instanceof String s && s.matches("[a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+(,\\d+)+"));
            builder.pop();

            builder.push("legendary_limit");
            legendaryEnabled = builder.comment("启用传奇词条数量限制")
                    .define("enabled", false);
            legendaryBypassVanillaGate = builder.comment("绕过原版传奇词条等级门槛")
                    .define("bypass_vanilla_gate", false);
            legendaryUnlimited = builder.comment("超过此难度取消传奇数量限制")
                    .defineInRange("unlimited_threshold", 2000, 0, Integer.MAX_VALUE);
            legendaryThresholds = builder.comment("格式: \"难度,最大数量\"",
                    "例: \"200,1\" = 难度 >= 200 时允许 1 个传奇词条")
                    .defineList("thresholds", List.of(),
                            e -> e instanceof String s && s.matches("\\d+,\\d+"));
            extraLegendaryIds = builder.comment("额外视为传奇的词条 ID")
                    .defineList("extra_legendary_ids", List.of(), e -> e instanceof String);
            builder.pop();

            builder.push("trait_exclusion");
            exclusionEnabled = builder.comment("启用词条互斥")
                    .define("enabled", true);
            exclusionGroups = builder.comment("格式: \"规则,词条1,词条2,...\"",
                    "规则 = \"roll\" (随机保留) 或 \"first\" (保留第一个)")
                    .defineList("groups",
                            List.of("first,l2hostility:moonwalk,l2hostility:gravity"),
                            e -> e instanceof String s && s.contains(","));
            builder.pop();

            builder.push("trait_generation");
            disableNonPresetTraits = builder.comment("仅保留预设词条")
                    .define("disable_non_preset_traits", false);
            disableAllTraits = builder.comment("禁用所有词条生成")
                    .define("disable_all_traits", false);
            disableMobLevel = builder.comment("禁用生物等级")
                    .define("disable_mob_level", false);
            builder.pop();

            builder.push("player_trait");
            playerMaxTraits = builder.comment("玩家最大词条种类数，-1 无上限")
                    .defineInRange("max_traits", -1, -1, 114514);
            playerSelfTraitEnabled = builder.comment("启用玩家自我词条")
                    .define("self_enabled", true);
            playerSelfTraitBalanceEnabled = builder.comment("启用自我词条平衡模式")
                    .define("self_balance_mode", false);
            playerSelfTraitBudgetRatio = builder.comment("自我词条预算倍率")
                    .defineInRange("self_budget_ratio", 1.0, 0.0, 10.0);
            playerSelfTraitCostMode = builder.comment("自我词条消耗模式",
                    "1 = 正常: 每次消耗 1 个",
                    "2 = 叠加: 消耗 (当前等级 + 1) 个",
                    "3 = 指数: 消耗 2^当前等级 个（依次为 1、2、4、8……）")
                    .defineInRange("self_cost_mode", 1, 1, 3);
            playerTraitOverrides = builder.comment("格式: \"词条id,最低等级,消耗\"",
                    "例: \"l2hostility:reprint,100,200\"")
                    .defineList("overrides", List.of(), e -> e instanceof String s && s.contains(","));
            playerTraitLimitEnabled = builder.comment("启用生物词条消耗上限")
                    .define("mob_limit_enabled", false);
            playerTraitBudgetRatio = builder.comment("生物词条预算倍率")
                    .defineInRange("mob_limit_budget_ratio", 1.0, 0.0, 10.0);
            builder.pop();
        }
    }

    // ==================== 解析 ====================

    public static List<int[]> getLevelThresholds() {
        if (parsedLevelThresholds == null) {
            parsedLevelThresholds = parseThresholds(COMMON.levelCapThresholds.get());
        }
        return parsedLevelThresholds;
    }

    public static Map<String, int[]> getPerTraitThresholds() {
        if (parsedPerTraitThresholds == null) {
            parsedPerTraitThresholds = parsePerTraitThresholds(COMMON.levelCapPerTrait.get());
        }
        return parsedPerTraitThresholds;
    }

    static Map<String, int[]> parsePerTraitThresholds(List<? extends String> raw) {
        Map<String, int[]> result = new LinkedHashMap<>();
        for (String entry : raw) {
            int firstComma = entry.indexOf(',');
            if (firstComma < 0) continue;
            String traitId = entry.substring(0, firstComma);
            String[] parts = entry.substring(firstComma + 1).split(",");
            int[] arr = new int[parts.length];
            boolean valid = true;
            for (int i = 0; i < parts.length; i++) {
                try {
                    arr[i] = Integer.parseInt(parts[i].trim());
                    if (i > 0) arr[i] = Math.max(arr[i - 1], arr[i]);
                } catch (NumberFormatException e) {
                    valid = false;
                    break;
                }
            }
            if (valid) result.put(traitId, arr);
        }
        return result;
    }

    public static int applyPerTraitLevelCap(int rolledRank, int globalCap,
                                            int difficulty, int[] thresholds) {
        int maxRank = globalCap;
        if (thresholds != null) {
            for (int i = 0; i < thresholds.length; i++) {
                if (difficulty < thresholds[i]) {
                    maxRank = Math.min(maxRank, i + 1);
                    break;
                }
            }
        }
        return Math.min(rolledRank, maxRank);
    }

    public static List<int[]> getLegendaryThresholds() {
        if (parsedLegendaryThresholds == null) {
            parsedLegendaryThresholds = parseThresholds(COMMON.legendaryThresholds.get());
        }
        return parsedLegendaryThresholds;
    }

    public static Set<String> getExtraLegendaryIds() {
        if (parsedExtraLegendaryIds == null) {
            parsedExtraLegendaryIds = new LinkedHashSet<>(COMMON.extraLegendaryIds.get());
        }
        return parsedExtraLegendaryIds;
    }

    public static List<ExclusionGroup> getExclusionGroups() {
        if (parsedExclusionGroups == null) {
            parsedExclusionGroups = parseExclusionGroups(COMMON.exclusionGroups.get());
        }
        return parsedExclusionGroups;
    }

    static List<ExclusionGroup> parseExclusionGroups(List<? extends String> raw) {
        List<ExclusionGroup> result = new ArrayList<>();
        for (String entry : raw) {
            String[] parts = entry.split(",");
            if (parts.length < 2) continue;
            String rule = parts[0].trim().toLowerCase(Locale.ROOT);
            if (!"first".equals(rule) && !"roll".equals(rule)) {
                LOGGER.warn("Ignoring exclusion group with unknown rule '{}': {}", parts[0].trim(), entry);
                continue;
            }
            Set<String> traits = new LinkedHashSet<>();
            for (int i = 1; i < parts.length; i++) {
                String id = parts[i].trim();
                if (ResourceLocation.tryParse(id) != null) traits.add(id);
            }
            if (traits.size() < 2) {
                LOGGER.warn("Ignoring exclusion group with fewer than two distinct valid trait IDs: {}", entry);
                continue;
            }
            result.add(new ExclusionGroup(rule, new ArrayList<>(traits)));
        }
        return result;
    }

    private static List<int[]> parseThresholds(List<? extends String> raw) {
        List<int[]> result = new ArrayList<>();
        for (String entry : raw) {
            String[] parts = entry.split(",");
            if (parts.length == 2) {
                try {
                    result.add(new int[]{
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim())
                    });
                } catch (NumberFormatException ignored) {}
            }
        }
        result.sort(Comparator.comparingInt(a -> a[0]));
        for (int i = 1; i < result.size(); i++) {
            result.get(i)[1] = Math.max(result.get(i - 1)[1], result.get(i)[1]);
        }
        return result;
    }

    public static int getThreshold(List<int[]> thresholds, int diff) {
        int result = 1;
        for (int[] t : thresholds) {
            if (diff >= t[0]) result = t[1];
        }
        return result;
    }

    // ==================== Getters ====================

    public static boolean isReprintLinearEnabled() {
        return COMMON.reprintLinearEnabled.get();
    }

    public static double getReprintDamage() {
        return COMMON.reprintDamageFactor.get();
    }

    public static double getAntiReprintReduction() {
        return COMMON.antiReprintReduction.get();
    }

    public static boolean isAdaptiveLinearEnabled() {
        return COMMON.adaptiveLinearEnabled.get();
    }

    public static double getAdaptiveReductionPerStack() {
        return COMMON.adaptiveReductionPerStack.get();
    }

    public static double getAdaptiveMaxReduction() {
        return COMMON.adaptiveMaxReduction.get();
    }

    public static boolean isDetectorGlassesRevealEnabled() {
        return COMMON.detectorGlassesReveal.get();
    }

    public static int getDetectorGlassesRange() {
        return COMMON.detectorGlassesRange.get();
    }

    public static double getWalkingBootsMovementSpeedCap() {
        return COMMON.walkingBootsMovementSpeedCap.get();
    }

    public static boolean isOldDispellEnabled() {
        return COMMON.oldDispell.get();
    }

    public static boolean isOldDementorEnabled() {
        return COMMON.oldDementor.get();
    }

    public static int getUndyingMaxResurrections() {
        return COMMON.undyingMaxResurrections.get();
    }

    public static int getUndyingSealDuration() {
        return COMMON.undyingSealDuration.get();
    }

    public static int getSealDurationMode() {
        return COMMON.sealDurationMode.get();
    }

    public static int getSealDurationLinear() {
        return COMMON.sealDurationLinear.get();
    }

    public static List<Integer> getSealDurationArray() {
        if (parsedSealDurationArray == null) {
            parsedSealDurationArray = new ArrayList<>(COMMON.sealDurationArray.get());
        }
        return parsedSealDurationArray;
    }

    public static int getSealDurationSeconds(int level) {
        return resolveSealDurationSeconds(
                getSealDurationMode(), getSealDurationLinear(), getSealDurationArray(), level);
    }

    // ==================== Ragnarok ====================

    public static int getRagnarokCount(int level) {
        List<? extends Integer> arr = COMMON.ragnarokCountArray.get();
        if (!arr.isEmpty()) {
            int idx = Math.min(level, arr.size()) - 1;
            return arr.get(Math.max(0, idx));
        }
        return level;
    }

    public static int getRagnarokTime(int level) {
        List<? extends Integer> arr = COMMON.ragnarokTimeArray.get();
        if (!arr.isEmpty()) {
            if (level <= arr.size()) return arr.get(level - 1);
            int last = arr.get(arr.size() - 1);
            return last + (level - arr.size()) * 100;
        }
        return dev.xkmc.l2hostility.init.data.LHConfig.COMMON.ragnarokTime.get() * level;
    }

	public static int getKillerAuraDamage(int level) {
		List<? extends Integer> arr = COMMON.killerAuraDamageArray.get();
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1);
		return dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraDamage.get() * level;
	}

	public static int getKillerAuraInterval(int level) {
		List<? extends Integer> arr = COMMON.killerAuraIntervalArray.get();
		int interval = !arr.isEmpty()
				? arr.get(Math.min(level, arr.size()) - 1)
				: dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraInterval.get() / level;
		return sanitizeKillerAuraInterval(interval);
	}

	static boolean isPositiveInteger(Object value) {
		return value instanceof Integer integer && integer > 0;
	}

	static boolean isNonNegativeInteger(Object value) {
		return value instanceof Integer integer && integer >= 0;
	}

	static int sanitizeKillerAuraInterval(int interval) {
		return Math.max(1, interval);
	}

	public static int getDispellTime(int level) {
		List<? extends Integer> arr = COMMON.dispellTimeArray.get();
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1);
		return dev.xkmc.l2hostility.init.data.LHConfig.COMMON.dispellTime.get() * level;
	}

	public static int getDispellCount(int level) {
		List<? extends Integer> arr = COMMON.dispellCountArray.get();
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1);
		return level;
	}

	public static double getDrainDamage(int level) {
		List<? extends Integer> arr = COMMON.drainDamageArray.get();
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1) / 100.0;
		return dev.xkmc.l2hostility.init.data.LHConfig.COMMON.drainDamage.get() * level;
	}

	public static double getDrainDuration(int level) {
		List<? extends Integer> arr = COMMON.drainDurationArray.get();
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1) / 100.0;
		return dev.xkmc.l2hostility.init.data.LHConfig.COMMON.drainDuration.get() * level;
	}

	public static int getDrainDurationMax(int level) {
		List<? extends Integer> arr = COMMON.drainDurationMaxArray.get();
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1) * 20;
		return level * dev.xkmc.l2hostility.init.data.LHConfig.COMMON.drainDurationMax.get();
	}

	public static int getDrainCount(int level) {
		List<? extends Integer> arr = COMMON.drainCountArray.get();
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1);
		return level;
	}
    public static boolean isExclusionEnabled() {
        return COMMON.exclusionEnabled.get();
    }

    public static boolean isDisableNonPresetTraits() {
        return COMMON.disableNonPresetTraits.get();
    }

    public static boolean isDisableAllTraits() {
        return COMMON.disableAllTraits.get();
    }

    public static boolean isDisableMobLevel() {
        return COMMON.disableMobLevel.get();
    }

    public static int getPlayerMaxTraits() {
        return COMMON.playerMaxTraits.get();
    }

    public static boolean isPlayerSelfTraitEnabled() {
        return COMMON.playerSelfTraitEnabled.get();
    }

    public static boolean isPlayerSelfTraitBalanceEnabled() {
        return COMMON.playerSelfTraitBalanceEnabled.get();
    }

    public static double getPlayerSelfTraitBudgetRatio() {
        return COMMON.playerSelfTraitBudgetRatio.get();
    }

    public static int getPlayerSelfTraitCostMode() {
        return COMMON.playerSelfTraitCostMode.get();
    }

    public static int getUpgradeCost(int currentLevel, int maxStackSize) {
        return TraitCostHelper.upgradeCost(getPlayerSelfTraitCostMode(), currentLevel, maxStackSize);
    }

    public static int getUnloadRefund(int currentLevel, int maxStackSize) {
        return TraitCostHelper.singleRefund(getPlayerSelfTraitCostMode(), currentLevel, maxStackSize);
    }

    public static int getTotalUnloadRefund(int currentLevel, int maxStackSize) {
        return TraitCostHelper.totalRefund(getPlayerSelfTraitCostMode(), currentLevel, maxStackSize);
    }

    public static boolean isPlayerTraitLimitEnabled() {
        return COMMON.playerTraitLimitEnabled.get();
    }

    public static double getPlayerTraitBudgetRatio() {
        return COMMON.playerTraitBudgetRatio.get();
    }

    public record ExclusionGroup(String rule, List<String> traitIds) {
        public ExclusionGroup {
            traitIds = List.copyOf(traitIds);
        }
    }

    public record PlayerTraitOverride(int minLevel, int cost) {}

    public static Map<String, PlayerTraitOverride> getPlayerTraitOverrides() {
        if (parsedPlayerTraitOverrides == null) {
            parsedPlayerTraitOverrides = parsePlayerTraitOverrides(COMMON.playerTraitOverrides.get());
        }
        return parsedPlayerTraitOverrides;
    }

    static Map<String, PlayerTraitOverride> parsePlayerTraitOverrides(List<? extends String> values) {
        Map<String, PlayerTraitOverride> result = new LinkedHashMap<>();
        for (String entry : values) {
            String[] parts = entry.split(",", -1);
            if (parts.length != 3) {
                LOGGER.warn("Ignoring malformed player trait override: {}", entry);
                continue;
            }
            String traitId = parts[0].trim();
            if (ResourceLocation.tryParse(traitId) == null) {
                LOGGER.warn("Ignoring player trait override with invalid trait ID: {}", entry);
                continue;
            }
            try {
                int minLevel = Integer.parseInt(parts[1].trim());
                int cost = Integer.parseInt(parts[2].trim());
                if (minLevel < 0 || cost < 0) {
                    LOGGER.warn("Ignoring player trait override with negative values: {}", entry);
                    continue;
                }
                result.put(traitId, new PlayerTraitOverride(minLevel, cost));
            } catch (NumberFormatException ignored) {
                LOGGER.warn("Ignoring player trait override with non-integer values: {}", entry);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static void installDisplaySnapshot(CompoundTag tag) {
        validateDisplaySnapshot(tag);
        displaySnapshot = new DisplaySnapshot(
                readBoolean(tag, "reprintLinearEnabled"),
                readDouble(tag, "reprintDamageFactor"),
                tag.contains("antiReprintReduction", Tag.TAG_ANY_NUMERIC)
                        ? tag.getDouble("antiReprintReduction") : null,
                readBoolean(tag, "adaptiveLinearEnabled"),
                readDouble(tag, "adaptiveReductionPerStack"),
                readDouble(tag, "adaptiveMaxReduction"),
                readBoolean(tag, "detectorGlassesReveal"),
                readInteger(tag, "detectorGlassesRange"),
                readBoolean(tag, "oldDispell"),
                readBoolean(tag, "oldDementor"),
                readInteger(tag, "undyingMaxResurrections"),
                readInteger(tag, "undyingSealDuration"),
                readInteger(tag, "sealDurationMode"),
                readInteger(tag, "sealDurationLinear"),
                readIntList(tag, "sealDurationArray"),
                readIntList(tag, "dispellTimeArray"),
                readInteger(tag, "dispellBaseTime"),
                readIntList(tag, "dispellCountArray"),
                readIntList(tag, "ragnarokCountArray"),
                readIntList(tag, "ragnarokTimeArray"),
                readInteger(tag, "ragnarokBaseTime"),
                readIntList(tag, "killerAuraDamageArray"),
                readInteger(tag, "killerAuraBaseDamage"),
                readIntList(tag, "killerAuraIntervalArray"),
                readInteger(tag, "killerAuraBaseInterval"),
                readInteger(tag, "killerAuraRange"),
                readInteger(tag, "bottleOfCurseLevel"),
                readIntList(tag, "drainDamageArray"),
                readDouble(tag, "drainBaseDamage"),
                readIntList(tag, "drainDurationArray"),
                readDouble(tag, "drainBaseDuration"),
                readIntList(tag, "drainDurationMaxArray"),
                readInteger(tag, "drainBaseDurationMax"),
                readIntList(tag, "drainCountArray"),
                readBoolean(tag, "levelCapEnabled"),
                readInteger(tag, "levelCapUnlimited"),
                readThresholds(tag, "levelCapThresholds"),
                readBoolean(tag, "legendaryEnabled"),
                readInteger(tag, "legendaryUnlimited"),
                readThresholds(tag, "legendaryThresholds"),
                readStringSet(tag, "extraLegendaryIds"),
                tag.contains("exclusionEnabled", Tag.TAG_BYTE) ? tag.getBoolean("exclusionEnabled") : null,
                readExclusionGroups(tag, "exclusionGroups"),
                tag.contains("playerSelfTraitEnabled", Tag.TAG_BYTE)
                        ? tag.getBoolean("playerSelfTraitEnabled") : null,
                tag.contains("playerSelfTraitBalanceEnabled", Tag.TAG_BYTE)
                        ? tag.getBoolean("playerSelfTraitBalanceEnabled") : null,
                tag.contains("playerSelfTraitBudgetRatio", Tag.TAG_ANY_NUMERIC)
                        ? tag.getDouble("playerSelfTraitBudgetRatio") : null,
                tag.contains("playerSelfTraitCostMode", Tag.TAG_ANY_NUMERIC)
                        ? tag.getInt("playerSelfTraitCostMode") : null,
                readPlayerTraitOverrides(tag, "playerTraitOverrides"));
    }

    public static CompoundTag createDisplaySnapshot() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("reprintLinearEnabled", COMMON.reprintLinearEnabled.get());
        tag.putDouble("reprintDamageFactor", COMMON.reprintDamageFactor.get());
        tag.putDouble("antiReprintReduction", COMMON.antiReprintReduction.get());
        tag.putBoolean("adaptiveLinearEnabled", COMMON.adaptiveLinearEnabled.get());
        tag.putDouble("adaptiveReductionPerStack", COMMON.adaptiveReductionPerStack.get());
        tag.putDouble("adaptiveMaxReduction", COMMON.adaptiveMaxReduction.get());
        tag.putBoolean("detectorGlassesReveal", COMMON.detectorGlassesReveal.get());
        tag.putInt("detectorGlassesRange", COMMON.detectorGlassesRange.get());
        tag.putBoolean("oldDispell", COMMON.oldDispell.get());
        tag.putBoolean("oldDementor", COMMON.oldDementor.get());
        tag.putInt("undyingMaxResurrections", COMMON.undyingMaxResurrections.get());
        tag.putInt("undyingSealDuration", COMMON.undyingSealDuration.get());
        tag.putInt("sealDurationMode", COMMON.sealDurationMode.get());
        tag.putInt("sealDurationLinear", COMMON.sealDurationLinear.get());
        putIntList(tag, "sealDurationArray", COMMON.sealDurationArray.get());
        putIntList(tag, "dispellTimeArray", COMMON.dispellTimeArray.get());
        tag.putInt("dispellBaseTime",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.dispellTime.get());
        putIntList(tag, "dispellCountArray", COMMON.dispellCountArray.get());
        putIntList(tag, "ragnarokCountArray", COMMON.ragnarokCountArray.get());
        putIntList(tag, "ragnarokTimeArray", COMMON.ragnarokTimeArray.get());
        tag.putInt("ragnarokBaseTime",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.ragnarokTime.get());
        putIntList(tag, "killerAuraDamageArray", COMMON.killerAuraDamageArray.get());
        tag.putInt("killerAuraBaseDamage",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraDamage.get());
        putIntList(tag, "killerAuraIntervalArray", COMMON.killerAuraIntervalArray.get());
        tag.putInt("killerAuraBaseInterval",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraInterval.get());
        tag.putInt("killerAuraRange",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraRange.get());
        tag.putInt("bottleOfCurseLevel",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.bottleOfCurseLevel.get());
        putIntList(tag, "drainDamageArray", COMMON.drainDamageArray.get());
        tag.putDouble("drainBaseDamage",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.drainDamage.get());
        putIntList(tag, "drainDurationArray", COMMON.drainDurationArray.get());
        tag.putDouble("drainBaseDuration",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.drainDuration.get());
        putIntList(tag, "drainDurationMaxArray", COMMON.drainDurationMaxArray.get());
        tag.putInt("drainBaseDurationMax",
                dev.xkmc.l2hostility.init.data.LHConfig.COMMON.drainDurationMax.get());
        putIntList(tag, "drainCountArray", COMMON.drainCountArray.get());
        tag.putBoolean("levelCapEnabled", COMMON.levelCapEnabled.get());
        tag.putInt("levelCapUnlimited", COMMON.levelCapUnlimited.get());
        putStringList(tag, "levelCapThresholds", COMMON.levelCapThresholds.get());
        tag.putBoolean("legendaryEnabled", COMMON.legendaryEnabled.get());
        tag.putInt("legendaryUnlimited", COMMON.legendaryUnlimited.get());
        putStringList(tag, "legendaryThresholds", COMMON.legendaryThresholds.get());
        putStringList(tag, "extraLegendaryIds", COMMON.extraLegendaryIds.get());
        tag.putBoolean("exclusionEnabled", COMMON.exclusionEnabled.get());
        putStringList(tag, "exclusionGroups", COMMON.exclusionGroups.get());
        tag.putBoolean("playerSelfTraitEnabled", COMMON.playerSelfTraitEnabled.get());
        tag.putBoolean("playerSelfTraitBalanceEnabled", COMMON.playerSelfTraitBalanceEnabled.get());
        tag.putDouble("playerSelfTraitBudgetRatio", COMMON.playerSelfTraitBudgetRatio.get());
        tag.putInt("playerSelfTraitCostMode", COMMON.playerSelfTraitCostMode.get());
        putStringList(tag, "playerTraitOverrides", COMMON.playerTraitOverrides.get());
        validateDisplaySnapshot(tag);
        return tag;
    }

    public static void clearDisplaySnapshot() {
        displaySnapshot = null;
    }

    public static boolean hasDisplaySnapshot() {
        return displaySnapshot != null;
    }

    public static double getDisplayAntiReprintReduction() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.antiReprintReduction() != null
                ? snapshot.antiReprintReduction() : getAntiReprintReduction();
    }

    public static boolean isDisplayReprintLinearEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.reprintLinearEnabled() != null
                ? snapshot.reprintLinearEnabled() : isReprintLinearEnabled();
    }

    public static double getDisplayReprintDamage() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.reprintDamageFactor() != null
                ? snapshot.reprintDamageFactor() : getReprintDamage();
    }

    public static boolean isDisplayAdaptiveLinearEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.adaptiveLinearEnabled() != null
                ? snapshot.adaptiveLinearEnabled() : isAdaptiveLinearEnabled();
    }

    public static double getDisplayAdaptiveReductionPerStack() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.adaptiveReductionPerStack() != null
                ? snapshot.adaptiveReductionPerStack() : getAdaptiveReductionPerStack();
    }

    public static double getDisplayAdaptiveMaxReduction() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.adaptiveMaxReduction() != null
                ? snapshot.adaptiveMaxReduction() : getAdaptiveMaxReduction();
    }

    public static boolean isDisplayDetectorGlassesRevealEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.detectorGlassesReveal() != null
                ? snapshot.detectorGlassesReveal() : isDetectorGlassesRevealEnabled();
    }

    public static int getDisplayDetectorGlassesRange() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.detectorGlassesRange() != null
                ? snapshot.detectorGlassesRange() : getDetectorGlassesRange();
    }

    public static boolean isDisplayOldDispellEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.oldDispell() != null
                ? snapshot.oldDispell() : isOldDispellEnabled();
    }

    public static boolean isDisplayOldDementorEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.oldDementor() != null
                ? snapshot.oldDementor() : isOldDementorEnabled();
    }

    public static int getDisplayUndyingMaxResurrections() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.undyingMaxResurrections() != null
                ? snapshot.undyingMaxResurrections() : getUndyingMaxResurrections();
    }

    public static int getDisplayUndyingSealDuration() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.undyingSealDuration() != null
                ? snapshot.undyingSealDuration() : getUndyingSealDuration();
    }

    public static int getDisplaySealDurationSeconds(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        int mode = snapshot != null && snapshot.sealDurationMode() != null
                ? snapshot.sealDurationMode() : getSealDurationMode();
        int linear = snapshot != null && snapshot.sealDurationLinear() != null
                ? snapshot.sealDurationLinear() : getSealDurationLinear();
        List<Integer> values = snapshot != null && snapshot.sealDurationArray() != null
                ? snapshot.sealDurationArray() : getSealDurationArray();
        return resolveSealDurationSeconds(mode, linear, values, level);
    }

    public static int getDisplayDispellTime(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.dispellTimeArray();
        if (values == null) return getDispellTime(level);
        if (values.isEmpty()) {
            return snapshot.dispellBaseTime() == null
                    ? getDispellTime(level) : snapshot.dispellBaseTime() * level;
        }
        return valueAtLevel(values, level);
    }

    public static int getDisplayDispellCount(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.dispellCountArray();
        if (values == null) return getDispellCount(level);
        return values.isEmpty() ? level : valueAtLevel(values, level);
    }

    public static int getDisplayRagnarokCount(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.ragnarokCountArray();
        if (values == null) return getRagnarokCount(level);
        return values.isEmpty() ? level : valueAtLevel(values, level);
    }

    public static int getDisplayRagnarokTime(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.ragnarokTimeArray();
        if (values == null) return getRagnarokTime(level);
        if (values.isEmpty()) {
            return snapshot.ragnarokBaseTime() == null
                    ? getRagnarokTime(level) : snapshot.ragnarokBaseTime() * level;
        }
        if (level <= values.size()) return values.get(Math.max(1, level) - 1);
        return values.get(values.size() - 1) + (level - values.size()) * 100;
    }

    public static int getDisplayKillerAuraDamage(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.killerAuraDamageArray();
        if (values == null) return getKillerAuraDamage(level);
        if (values.isEmpty()) {
            return snapshot.killerAuraBaseDamage() == null
                    ? getKillerAuraDamage(level) : snapshot.killerAuraBaseDamage() * level;
        }
        return valueAtLevel(values, level);
    }

    public static int getDisplayKillerAuraInterval(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.killerAuraIntervalArray();
        if (values == null) return getKillerAuraInterval(level);
        if (values.isEmpty()) {
            return snapshot.killerAuraBaseInterval() == null
                    ? getKillerAuraInterval(level)
                    : sanitizeKillerAuraInterval(snapshot.killerAuraBaseInterval() / Math.max(1, level));
        }
        return sanitizeKillerAuraInterval(valueAtLevel(values, level));
    }

    public static int getDisplayKillerAuraRange() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.killerAuraRange() != null
                ? snapshot.killerAuraRange()
                : dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraRange.get();
    }

    public static int getDisplayBottleOfCurseLevel() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.bottleOfCurseLevel() != null
                ? snapshot.bottleOfCurseLevel()
                : dev.xkmc.l2hostility.init.data.LHConfig.COMMON.bottleOfCurseLevel.get();
    }

    public static double getDisplayDrainDamage(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.drainDamageArray();
        if (values == null) return getDrainDamage(level);
        if (values.isEmpty()) {
            return snapshot.drainBaseDamage() == null
                    ? getDrainDamage(level) : snapshot.drainBaseDamage() * level;
        }
        return valueAtLevel(values, level) / 100.0;
    }

    public static double getDisplayDrainDuration(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.drainDurationArray();
        if (values == null) return getDrainDuration(level);
        if (values.isEmpty()) {
            return snapshot.drainBaseDuration() == null
                    ? getDrainDuration(level) : snapshot.drainBaseDuration() * level;
        }
        return valueAtLevel(values, level) / 100.0;
    }

    public static int getDisplayDrainDurationMax(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.drainDurationMaxArray();
        if (values == null) return getDrainDurationMax(level);
        if (values.isEmpty()) {
            return snapshot.drainBaseDurationMax() == null
                    ? getDrainDurationMax(level) : snapshot.drainBaseDurationMax() * level;
        }
        return valueAtLevel(values, level) * 20;
    }

    public static int getDisplayDrainCount(int level) {
        DisplaySnapshot snapshot = displaySnapshot;
        List<Integer> values = snapshot == null ? null : snapshot.drainCountArray();
        if (values == null) return getDrainCount(level);
        return values.isEmpty() ? level : valueAtLevel(values, level);
    }

    public static Set<String> getDisplayExtraLegendaryIds() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.extraLegendaryIds() != null
                ? snapshot.extraLegendaryIds() : getExtraLegendaryIds();
    }

    public static boolean isDisplayLevelCapEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.levelCapEnabled() != null
                ? snapshot.levelCapEnabled() : COMMON.levelCapEnabled.get();
    }

    public static int getDisplayLevelCapUnlimited() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.levelCapUnlimited() != null
                ? snapshot.levelCapUnlimited() : COMMON.levelCapUnlimited.get();
    }

    public static List<int[]> getDisplayLevelThresholds() {
        DisplaySnapshot snapshot = displaySnapshot;
        List<int[]> values = snapshot != null && snapshot.levelCapThresholds() != null
                ? snapshot.levelCapThresholds() : getLevelThresholds();
        return copyThresholds(values);
    }

    public static boolean isDisplayLegendaryEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.legendaryEnabled() != null
                ? snapshot.legendaryEnabled() : COMMON.legendaryEnabled.get();
    }

    public static int getDisplayLegendaryUnlimited() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.legendaryUnlimited() != null
                ? snapshot.legendaryUnlimited() : COMMON.legendaryUnlimited.get();
    }

    public static List<int[]> getDisplayLegendaryThresholds() {
        DisplaySnapshot snapshot = displaySnapshot;
        List<int[]> values = snapshot != null && snapshot.legendaryThresholds() != null
                ? snapshot.legendaryThresholds() : getLegendaryThresholds();
        return copyThresholds(values);
    }

    public static boolean isDisplayExclusionEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.exclusionEnabled() != null
                ? snapshot.exclusionEnabled() : isExclusionEnabled();
    }

    public static List<ExclusionGroup> getDisplayExclusionGroups() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.exclusionGroups() != null
                ? snapshot.exclusionGroups() : getExclusionGroups();
    }

    public static boolean isDisplayPlayerSelfTraitBalanceEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.playerSelfTraitBalanceEnabled() != null
                ? snapshot.playerSelfTraitBalanceEnabled() : isPlayerSelfTraitBalanceEnabled();
    }

    public static boolean isDisplayPlayerSelfTraitEnabled() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.playerSelfTraitEnabled() != null
                ? snapshot.playerSelfTraitEnabled() : isPlayerSelfTraitEnabled();
    }

    public static double getDisplayPlayerSelfTraitBudgetRatio() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.playerSelfTraitBudgetRatio() != null
                ? snapshot.playerSelfTraitBudgetRatio() : getPlayerSelfTraitBudgetRatio();
    }

    public static int getDisplayUpgradeCost(int currentLevel, int maxStackSize) {
        DisplaySnapshot snapshot = displaySnapshot;
        int mode = snapshot != null && snapshot.playerSelfTraitCostMode() != null
                ? snapshot.playerSelfTraitCostMode() : getPlayerSelfTraitCostMode();
        return TraitCostHelper.upgradeCost(mode, currentLevel, maxStackSize);
    }

    public static Map<String, PlayerTraitOverride> getDisplayPlayerTraitOverrides() {
        DisplaySnapshot snapshot = displaySnapshot;
        return snapshot != null && snapshot.playerTraitOverrides() != null
                ? snapshot.playerTraitOverrides() : getPlayerTraitOverrides();
    }

    private static int valueAtLevel(List<Integer> values, int level) {
        return values.get(Math.max(0, Math.min(level, values.size()) - 1));
    }

    private static int resolveSealDurationSeconds(
            int mode, int linear, List<Integer> values, int level) {
        if (mode == 2 && !values.isEmpty()) {
            if (level <= values.size()) return values.get(level - 1);
            return values.get(values.size() - 1) + (level - values.size()) * linear;
        }
        return level * linear;
    }

    private static List<int[]> copyThresholds(List<int[]> values) {
        return values.stream().map(int[]::clone).toList();
    }

    public static UpstreamDisplayConfig getUpstreamDisplayConfig() {
        var common = dev.xkmc.l2hostility.init.data.LHConfig.COMMON;
        return new UpstreamDisplayConfig(
                common.bottleOfCurseLevel.get(),
                common.dispellTime.get(),
                common.ragnarokTime.get(),
                common.killerAuraDamage.get(),
                common.killerAuraInterval.get(),
                common.killerAuraRange.get(),
                common.drainDamage.get(),
                common.drainDuration.get(),
                common.drainDurationMax.get());
    }

    private static List<Integer> readIntList(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_INT_ARRAY)) return null;
        return Arrays.stream(tag.getIntArray(key)).boxed().toList();
    }

    private static Integer readInteger(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : null;
    }

    private static Double readDouble(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : null;
    }

    private static Boolean readBoolean(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : null;
    }

    private static List<int[]> readThresholds(CompoundTag tag, String key) {
        List<String> values = readStrings(tag, key);
        return values == null ? null : List.copyOf(parseThresholds(values));
    }

    public static void validateDisplaySnapshot(CompoundTag tag) {
        for (String key : List.of(
                "sealDurationArray", "dispellTimeArray", "dispellCountArray",
                "ragnarokCountArray", "ragnarokTimeArray",
                "killerAuraDamageArray", "killerAuraIntervalArray",
                "drainDamageArray", "drainDurationArray",
                "drainDurationMaxArray", "drainCountArray")) {
            if (tag.contains(key, Tag.TAG_INT_ARRAY)
                    && tag.getIntArray(key).length > MAX_DISPLAY_CONFIG_ENTRIES) {
                throw new IllegalArgumentException("Too many display config entries for " + key);
            }
        }
        int totalStringLength = 0;
        for (String key : List.of(
                "levelCapThresholds", "legendaryThresholds", "extraLegendaryIds",
                "exclusionGroups", "playerTraitOverrides")) {
            if (!tag.contains(key, Tag.TAG_LIST)) continue;
            ListTag values = tag.getList(key, Tag.TAG_STRING);
            if (values.size() > MAX_DISPLAY_CONFIG_ENTRIES) {
                throw new IllegalArgumentException("Too many display config entries for " + key);
            }
            for (int i = 0; i < values.size(); i++) {
                int length = values.getString(i).length();
                if (length > MAX_DISPLAY_CONFIG_STRING_LENGTH) {
                    throw new IllegalArgumentException("Display config value is too long for " + key);
                }
                totalStringLength += length;
                if (totalStringLength > MAX_DISPLAY_CONFIG_TOTAL_STRING_LENGTH) {
                    throw new IllegalArgumentException("Display config strings exceed total size limit");
                }
            }
        }
    }

    private static void putIntList(CompoundTag tag, String key, List<? extends Integer> values) {
        tag.putIntArray(key, values.stream().mapToInt(Integer::intValue).toArray());
    }

    private static void putStringList(CompoundTag tag, String key, List<? extends String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(net.minecraft.nbt.StringTag.valueOf(value));
        tag.put(key, list);
    }

    private static List<String> readStrings(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) return null;
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        List<String> values = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) values.add(list.getString(i));
        return List.copyOf(values);
    }

    private static Set<String> readStringSet(CompoundTag tag, String key) {
        List<String> values = readStrings(tag, key);
        return values == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private static List<ExclusionGroup> readExclusionGroups(CompoundTag tag, String key) {
        List<String> values = readStrings(tag, key);
        return values == null ? null : List.copyOf(parseExclusionGroups(values));
    }

    private static Map<String, PlayerTraitOverride> readPlayerTraitOverrides(CompoundTag tag, String key) {
        List<String> values = readStrings(tag, key);
        return values == null ? null : parsePlayerTraitOverrides(values);
    }

    private record DisplaySnapshot(
            Boolean reprintLinearEnabled,
            Double reprintDamageFactor,
            Double antiReprintReduction,
            Boolean adaptiveLinearEnabled,
            Double adaptiveReductionPerStack,
            Double adaptiveMaxReduction,
            Boolean detectorGlassesReveal,
            Integer detectorGlassesRange,
            Boolean oldDispell,
            Boolean oldDementor,
            Integer undyingMaxResurrections,
            Integer undyingSealDuration,
            Integer sealDurationMode,
            Integer sealDurationLinear,
            List<Integer> sealDurationArray,
            List<Integer> dispellTimeArray,
            Integer dispellBaseTime,
            List<Integer> dispellCountArray,
            List<Integer> ragnarokCountArray,
            List<Integer> ragnarokTimeArray,
            Integer ragnarokBaseTime,
            List<Integer> killerAuraDamageArray,
            Integer killerAuraBaseDamage,
            List<Integer> killerAuraIntervalArray,
            Integer killerAuraBaseInterval,
            Integer killerAuraRange,
            Integer bottleOfCurseLevel,
            List<Integer> drainDamageArray,
            Double drainBaseDamage,
            List<Integer> drainDurationArray,
            Double drainBaseDuration,
            List<Integer> drainDurationMaxArray,
            Integer drainBaseDurationMax,
            List<Integer> drainCountArray,
            Boolean levelCapEnabled,
            Integer levelCapUnlimited,
            List<int[]> levelCapThresholds,
            Boolean legendaryEnabled,
            Integer legendaryUnlimited,
            List<int[]> legendaryThresholds,
            Set<String> extraLegendaryIds,
            Boolean exclusionEnabled,
            List<ExclusionGroup> exclusionGroups,
            Boolean playerSelfTraitEnabled,
            Boolean playerSelfTraitBalanceEnabled,
            Double playerSelfTraitBudgetRatio,
            Integer playerSelfTraitCostMode,
            Map<String, PlayerTraitOverride> playerTraitOverrides) {
    }

    public record UpstreamDisplayConfig(
            int bottleOfCurseLevel,
            int dispellTime,
            int ragnarokTime,
            int killerAuraDamage,
            int killerAuraInterval,
            int killerAuraRange,
            double drainDamage,
            double drainDuration,
            int drainDurationMax) {
    }

    public static void invalidateCaches() {
        parsedLevelThresholds = null;
        parsedPerTraitThresholds = null;
        parsedLegendaryThresholds = null;
        parsedExtraLegendaryIds = null;
        parsedExclusionGroups = null;
        parsedSealDurationArray = null;
        parsedPlayerTraitOverrides = null;
    }
}
