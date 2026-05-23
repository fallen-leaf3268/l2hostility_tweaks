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

        // === Reprint ===
        public final ForgeConfigSpec.BooleanValue reprintLinearEnabled;
        public final ForgeConfigSpec.DoubleValue reprintDamageFactor;
        public final ForgeConfigSpec.DoubleValue antiReprintReduction;

        // === Adaptive ===
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
        public final ForgeConfigSpec.IntValue traitSealDuration;

        // === HUD ===
        public final ForgeConfigSpec.BooleanValue showHud;

        // === 等级限制 ===
        public final ForgeConfigSpec.BooleanValue levelCapEnabled;
        public final ForgeConfigSpec.IntValue levelCapUnlimited;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> levelCapThresholds;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> levelCapPerTrait;

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
            reprintLinearEnabled = builder.comment("启用 Reprint 词条线性化",
                    "开启后: 附魔点数 = 附魔等级之和 (而非 2^(等级-1))")
                    .define("linear_enabled", false);
            reprintDamageFactor = builder.comment("每点附魔点数的增伤比例",
                    "默认 0.05 (每点 +5% 伤害)")
                    .defineInRange("damage_factor", 0.05, 0.0, 1.0);
            antiReprintReduction = builder.comment("复印抵抗附魔每级减伤比例",
                    "默认 0.02 (每级 -2% 受到 Reprint 生物的伤害)")
                    .defineInRange("counter_reduction", 0.02, 0.0, 1.0);
            builder.pop();

            builder.push("adaptive");
            adaptiveLinearEnabled = builder.comment("启用 Adaptive 词条线性叠加减伤",
                    "开启后: 每次受到同种伤害增加减伤，而非原版的 Math.pow 指数计算")
                    .define("enabled", true);
            adaptiveReductionPerStack = builder.comment("每层适应提供的减伤比例",
                    "默认 0.25 (每层 +25% 减伤，加法叠加)")
                    .defineInRange("reduction_per_stack", 0.25, 0.0, 1.0);
            adaptiveMaxReduction = builder.comment("适应词条最大减伤上限",
                    "默认 0.95 (最高 95% 减伤)")
                    .defineInRange("max_reduction", 0.95, 0.0, 1.0);
            builder.pop();

            builder.push("detector_glasses");
            detectorGlassesReveal = builder.comment("佩戴探测目镜时直接显示隐身生物（而非仅发光轮廓）")
                    .define("reveal_invisible", true);
            detectorGlassesRange = builder.comment("探测目镜显示隐身生物的范围（格）")
                    .defineInRange("reveal_range", 48, 1, 256);
            builder.pop();

            builder.push("legendary_defense");
            oldDispell = builder.comment("启用破魔词条（Dispell）的旧版免疫机制",
                    "开启后: 破魔词条的生物免疫魔法伤害")
                    .define("old_dispell", false);
            oldDementor = builder.comment("启用摄魂词条（Dementor）的旧版免疫机制",
                    "开启后: 摄魂词条的生物免疫非魔法伤害")
                    .define("old_dementor", false);
            builder.pop();

            builder.push("undying");
            undyingMaxResurrections = builder.comment("不死词条（Undying）最大重生次数",
                    "-1 = 无限制，正数 = 该生物最多重生的次数")
                    .defineInRange("max_resurrections", -1, -1, 114514);
            traitSealDuration = builder.comment("词条封印持续时间（秒）",
                    "-1 = 永久封印，正数 = 经过该时间后自动解封")
                    .defineInRange("seal_duration", 60, -1, 3600);
            builder.pop();

            builder.push("hud");
            showHud = builder.comment("是否显示自定义血条 HUD（仅对非玩家实体生效）")
                    .define("enabled", false);
            builder.pop();

            builder.push("level_cap");
            levelCapEnabled = builder.comment("启用词条等级阶梯限制")
                    .define("enabled", false);
            levelCapUnlimited = builder.comment("难度 >= 该值时取消所有词条等级上限")
                    .defineInRange("unlimited_threshold", 1000, 0, Integer.MAX_VALUE);
            levelCapThresholds = builder.comment("格式: \"难度,最高等级\"",
                    "例: \"200,2\" = 难度 >= 200 时最高等级为 2")
                    .defineList("thresholds", List.of(),
                            e -> e instanceof String s && s.matches("\\d+,\\d+"));
            levelCapPerTrait = builder.comment("格式: \"词条id,等级2所需难度,等级3所需难度,...\"",
                    "每个数字表示该等级需要的最低难度",
                    "例: \"l2hostility:repelling,100,200\" = lv2需难度100, lv3需难度200")
                    .defineList("per_trait", List.of(),
                            e -> e instanceof String s && s.matches("[a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+(,\\d+)+"));
            builder.pop();

            builder.push("legendary_limit");
            legendaryEnabled = builder.comment("启用传奇词条数量限制")
                    .define("enabled", false);
            legendaryUnlimited = builder.comment("难度 >= 该值时传奇词条数量无限制")
                    .defineInRange("unlimited_threshold", 2000, 0, Integer.MAX_VALUE);
            legendaryThresholds = builder.comment("格式: \"难度,最大数量\"",
                    "例: \"200,1\" = 难度 >= 200 时允许 1 个传奇词条")
                    .defineList("thresholds", List.of(),
                            e -> e instanceof String s && s.matches("\\d+,\\d+"));
            extraLegendaryIds = builder.comment("视为传奇词条的额外词条ID",
                    "这些词条将被视为传奇词条")
                    .defineList("extra_legendary_ids", List.of(), e -> e instanceof String);
            builder.pop();

            builder.push("trait_exclusion");
            exclusionEnabled = builder.comment("启用词条互斥")
                    .define("enabled", true);
            exclusionGroups = builder.comment("格式: \"规则,词条1,词条2,...\"",
                    "规则 = \"roll\" (随机保留一个) 或 \"first\" (存在时保留第一个)")
                    .defineList("groups",
                            List.of("first,l2hostility:moonwalk,l2hostility:gravity"),
                            e -> e instanceof String s && s.contains(","));
            builder.pop();

            builder.push("trait_generation");
            disableNonPresetTraits = builder.comment("关闭非预设词条生成",
                    "开启后: 仅保留数据包预设词条，随机生成的词条将被移除")
                    .define("disable_non_preset_traits", false);
            disableAllTraits = builder.comment("关闭所有词条生成",
                    "开启后: 生物不获得任何词条（包括预设）")
                    .define("disable_all_traits", false);
            disableMobLevel = builder.comment("关闭生物等级",
                    "开启后: 生物不获得等级和词条")
                    .define("disable_mob_level", false);
            builder.pop();

            builder.push("player_trait");
            playerMaxTraits = builder.comment("玩家最大词条数量",
                            "-1 = 无上限，0 = 禁止添加，正数 = 最大词条种类数")
                            .defineInRange("max_traits", -1, -1, 114514);
            playerSelfTraitEnabled = builder.comment("启用玩家自我词条功能",
                    "开启后: 玩家手持 TraitSymbol 蹲下右键给自己添加词条")
                    .define("self_enabled", true);
            playerSelfTraitBalanceEnabled = builder.comment("启用玩家自我词条平衡模式",
                    "开启后: 添加词条受玩家难度等级限制",
                    "词条最低等级: 玩家难度等级 >= 词条配置的 min_level 才可添加",
                    "词条消耗: 玩家所有词条总 cost 不得超过 玩家等级 × 预算倍率")
                    .define("self_balance_mode", false);
            playerSelfTraitBudgetRatio = builder.comment("自我词条预算倍率",
                    "预算 = 玩家难度等级 × 该倍率")
                    .defineInRange("self_budget_ratio", 1.0, 0.0, 10.0);
            playerSelfTraitCostMode = builder.comment("自我词条消耗模式",
                    "1 = 正常模式: 每次消耗 1 个词条物品",
                    "2 = 叠加模式: 消耗 (当前等级+1) 个词条物品",
                    "3 = 指数模式: 消耗 2^(当前等级-1) 个词条物品")
                    .defineInRange("self_cost_mode", 1, 1, 3);
            playerTraitOverrides = builder.comment("玩家词条独立配置",
                            "格式: \"词条id,最低等级要求,消耗\"",
                            "例: \"l2hostility:reprint,100,200\"",
                            "配置后的词条在玩家使用时将使用此配置替代原版 min_level 和 cost",
                            "互斥使用下方 trait_exclusion 统一配置")
                            .defineList("overrides", List.of(), e -> e instanceof String s && s.contains(","));
            playerTraitLimitEnabled = builder.comment("启用玩家给生物添加词条的消耗上限",
                    "开启后: 玩家右键给生物添加词条时，检测生物等级和已有词条消耗",
                    "若总消耗超过生物等级则无法添加")
                    .define("mob_limit_enabled", false);
            playerTraitBudgetRatio = builder.comment("生物添加词条预算倍率",
                    "实际预算 = 生物等级 × 该倍率")
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

    public static int getTraitSealDuration() {
        return COMMON.traitSealDuration.get();
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
		int mode = getPlayerSelfTraitCostMode();
		if (mode == 2) {
			return currentLevel;
		} else if (mode == 3) {
			return 1 << (currentLevel - 1);
		}
		return 1;
	}

	public static int getTotalUnloadRefund(int currentLevel) {
		int mode = getPlayerSelfTraitCostMode();
		if (mode == 2) {
			return currentLevel * (currentLevel + 1) / 2;
		} else if (mode == 3) {
			return (1 << currentLevel) - 1;
		}
		return currentLevel;
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
