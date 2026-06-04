package com.l2hostility_tweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class L2HConfig {

    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    private static List<int[]> parsedLevelThresholds;
    private static Map<String, int[]> parsedPerTraitThresholds;
    private static List<int[]> parsedLegendaryThresholds;
    private static Set<String> parsedExtraLegendaryIds;
    private static List<ExclusionGroup> parsedExclusionGroups;
    private static List<Integer> parsedSealDurationArray;
    private static Map<String, PlayerTraitOverride> parsedPlayerTraitOverrides;

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

        // === 旧版防御 ===
        public final ForgeConfigSpec.BooleanValue oldDispell;
        public final ForgeConfigSpec.BooleanValue oldDementor;

        // === 不死 ===
        public final ForgeConfigSpec.IntValue undyingMaxResurrections;
        public final ForgeConfigSpec.IntValue undyingSealDuration;

        // === HUD ===
        public final ForgeConfigSpec.BooleanValue showHud;

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
                    .defineList("duration_array", List.of(), e -> e instanceof Integer);
            builder.pop();

            builder.push("ragnarok");
            ragnarokCountArray = builder.comment("诸神黄昏数组模式配置",
                    "每级封印物品数量")
                    .defineList("count_array", List.of(), e -> e instanceof Integer);
            ragnarokTimeArray = builder.comment("每级封印时长 (tick)")
                    .defineList("time_array", List.of(), e -> e instanceof Integer);
            builder.pop();

            builder.push("killer_aura");
            killerAuraDamageArray = builder.comment("Killer Aura 数组配置",
                    "每级伤害")
                    .defineList("damage_array", List.of(), e -> e instanceof Integer);
            killerAuraIntervalArray = builder.comment("每级攻击间隔 (tick)")
                    .defineList("interval_array", List.of(), e -> e instanceof Integer);
            builder.pop();

            builder.push("dispell");
            dispellTimeArray = builder.comment("Dispell 数组配置",
                    "每级封印时长 (tick)")
                    .defineList("time_array", List.of(), e -> e instanceof Integer);
            dispellCountArray = builder.comment("每级封印物品数量")
                    .defineList("count_array", List.of(), e -> e instanceof Integer);
            builder.pop();

            builder.push("drain");
            drainDamageArray = builder.comment("Drain 数组配置",
                    "每级伤害加成")
                    .defineList("damage_array", List.of(), e -> e instanceof Integer);
            drainDurationArray = builder.comment("每级时长时间")
                    .defineList("duration_array", List.of(), e -> e instanceof Integer);
            drainDurationMaxArray = builder.comment("每级最高延长时间 (s)")
                    .defineList("duration_max_array", List.of(), e -> e instanceof Integer);
            drainCountArray = builder.comment("每级剥夺效果数量")
                    .defineList("count_array", List.of(), e -> e instanceof Integer);
            builder.pop();

            builder.push("undying");
            undyingMaxResurrections = builder.comment("不死词条最大重生次数，-1 无限制")
                    .defineInRange("max_resurrections", -1, -1, 114514);
            undyingSealDuration = builder.comment("不死词条耗尽后封印时长（秒），-1 永久，0 不封印")
                    .defineInRange("seal_duration", 0, -1, 3600);
            builder.pop();

            builder.push("hud");
            showHud = builder.comment("显示自定义血条 HUD")
                    .define("enabled", false);
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
                    "3 = 指数: 消耗 2^(当前等级 - 1) 个")
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
            parsedPerTraitThresholds = new LinkedHashMap<>();
            for (String entry : COMMON.levelCapPerTrait.get()) {
                int firstComma = entry.indexOf(',');
                if (firstComma < 0) continue;
                String traitId = entry.substring(0, firstComma);
                String[] parts = entry.substring(firstComma + 1).split(",");
                int[] arr = new int[parts.length];
                boolean valid = true;
                for (int i = 0; i < parts.length; i++) {
                    try { arr[i] = Integer.parseInt(parts[i].trim()); }
                    catch (NumberFormatException e) { valid = false; break; }
                }
                if (valid) parsedPerTraitThresholds.put(traitId, arr);
            }
        }
        return parsedPerTraitThresholds;
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
            parsedExclusionGroups = new ArrayList<>();
            for (String entry : COMMON.exclusionGroups.get()) {
                String[] parts = entry.split(",");
                if (parts.length < 2) continue;
                String rule = parts[0].trim();
                List<String> traits = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (!s.isEmpty()) traits.add(s);
                }
                if (!traits.isEmpty()) parsedExclusionGroups.add(new ExclusionGroup(rule, traits));
            }
        }
        return parsedExclusionGroups;
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
		if (!arr.isEmpty()) return arr.get(Math.min(level, arr.size()) - 1);
		return dev.xkmc.l2hostility.init.data.LHConfig.COMMON.killerAuraInterval.get() / level;
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

    public static int getUnloadRefund(int currentLevel) {
        int lv = Math.abs(currentLevel);
        int mode = getPlayerSelfTraitCostMode();
        if (mode == 2) {
            return lv;
        } else if (mode == 3) {
            return lv > 0 ? 1 << (lv - 1) : 0;
        }
        return lv > 0 ? 1 : 0;
    }

    public static int getTotalUnloadRefund(int currentLevel) {
        int lv = Math.abs(currentLevel);
        int mode = getPlayerSelfTraitCostMode();
        if (mode == 2) {
            return lv * (lv + 1) / 2;
        } else if (mode == 3) {
            return lv > 0 ? (1 << lv) - 1 : 0;
        }
        return lv;
    }

    public static boolean isPlayerTraitLimitEnabled() {
        return COMMON.playerTraitLimitEnabled.get();
    }

    public static double getPlayerTraitBudgetRatio() {
        return COMMON.playerTraitBudgetRatio.get();
    }

    public record ExclusionGroup(String rule, List<String> traitIds) {}

    public record PlayerTraitOverride(int minLevel, int cost) {}

    public static Map<String, PlayerTraitOverride> getPlayerTraitOverrides() {
        if (parsedPlayerTraitOverrides == null) {
            parsedPlayerTraitOverrides = new LinkedHashMap<>();
            for (String entry : COMMON.playerTraitOverrides.get()) {
                String[] parts = entry.split(",");
                if (parts.length < 3) continue;
                String traitId = parts[0].trim();
                if (traitId.isEmpty()) continue;
                try {
                    int minLevel = Integer.parseInt(parts[1].trim());
                    int cost = Integer.parseInt(parts[2].trim());
                    parsedPlayerTraitOverrides.put(traitId, new PlayerTraitOverride(minLevel, cost));
                } catch (NumberFormatException ignored) {}
            }
        }
        return parsedPlayerTraitOverrides;
    }

    public static void invalidatePlayerTraitOverrides() {
        parsedPlayerTraitOverrides = null;
    }
}
