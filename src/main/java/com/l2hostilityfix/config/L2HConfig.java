package com.l2hostilityfix.config;

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

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "l2_configs/l2hostilityfix.toml");
    }

    public static class Common {

        // === 等级上限 ===
        public final ForgeConfigSpec.BooleanValue levelCapEnabled;
        public final ForgeConfigSpec.IntValue levelCapUnlimited;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> levelCapThresholds;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> levelCapPerTrait;

        // === 传奇词条限制 ===
        public final ForgeConfigSpec.BooleanValue legendaryEnabled;
        public final ForgeConfigSpec.IntValue legendaryUnlimited;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> legendaryThresholds;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> extraLegendaryIds;

        // === 词条互斥 ===
        public final ForgeConfigSpec.BooleanValue exclusionEnabled;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> exclusionGroups;

        // === Reprint 线性化 ===
        public final ForgeConfigSpec.BooleanValue reprintLinearEnabled;
        public final ForgeConfigSpec.DoubleValue reprintDamageFactor;

        // === Adaptive 线性叠加减伤 ===
        public final ForgeConfigSpec.BooleanValue adaptiveLinearEnabled;
        public final ForgeConfigSpec.DoubleValue adaptiveReductionPerStack;
        public final ForgeConfigSpec.DoubleValue adaptiveMaxReduction;

        // === 复印抵抗附魔 ===
        public final ForgeConfigSpec.DoubleValue antiReprintReduction;

        // === HUD ===
        public final ForgeConfigSpec.BooleanValue showHud;

        Common(ForgeConfigSpec.Builder builder) {

            builder.push("reprint");
            reprintLinearEnabled = builder.comment("启用 Reprint 词条线性化",
                    "开启后: 附魔点数 = 附魔等级之和 (而非 2^(等级-1))")
                    .define("enabled", false);
            reprintDamageFactor = builder.comment("每点附魔点数的增伤比例",
                    "默认 0.05 (每点 +5% 伤害)")
                    .defineInRange("damage_factor", 0.05, 0.0, 1.0);
            builder.pop();

            builder.push("reprint_counter");
            antiReprintReduction = builder.comment("复印抵抗附魔每级减伤比例",
                    "默认 0.02 (每级 -2% 受到 Reprint 生物的伤害)",
                    "仅当附魔在盔甲槽位上时生效")
                    .defineInRange("reduction_per_level", 0.02, 0.0, 1.0);
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

            builder.push("hud");
            showHud = builder.comment("是否显示自定义血条 HUD").define("enabled", false);
            builder.pop();

            builder.push("level_cap");
            levelCapEnabled = builder.comment("是否启用词条等级阶梯限制")
                    .define("enabled", false);
            levelCapUnlimited = builder.comment("难度 >= 该值时取消所有词条等级上限")
                    .defineInRange("unlimited_threshold", 1000, 0, Integer.MAX_VALUE);
            levelCapThresholds = builder.comment("格式: \"难度,最高等级\"",
                            "例: \"200,2\" = 难度 >= 200 时最高等级为 2")
                    .defineList("thresholds",
                            List.of(),
                            e -> e instanceof String s && s.matches("\\d+,\\d+"));
            levelCapPerTrait = builder.comment("格式: \"词条id,等级2所需难度,等级3所需难度,...\"",
                            "每个数字表示该等级需要的最低难度",
                            "例: \"l2hostility:repelling,100,200\" = lv2需难度100, lv3需难度200")
                    .defineList("per_trait",
                            List.of(),
                            e -> e instanceof String s && s.matches("[a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+(,\\d+)+"));
            builder.pop();

            builder.push("legendary_limit");
            legendaryEnabled = builder.comment("是否启用传奇词条数量限制")
                    .define("enabled", false);
            legendaryUnlimited = builder.comment("难度 >= 该值时传奇词条数量无限制")
                    .defineInRange("unlimited_threshold", 2000, 0, Integer.MAX_VALUE);
            legendaryThresholds = builder.comment("格式: \"难度,最大数量\"",
                            "例: \"200,1\" = 难度 >= 200 时允许 1 个传奇词条")
                    .defineList("thresholds",
                            List.of(),
                            e -> e instanceof String s && s.matches("\\d+,\\d+"));
            extraLegendaryIds = builder.comment("视为传奇词条的额外词条ID",
                            "这些词条将被视为传奇词条")
                    .defineList("extra_legendary_ids", List.of(), e -> e instanceof String);
            builder.pop();

            builder.push("trait_exclusion");
            exclusionEnabled = builder.comment("是否启用词条互斥")
                    .define("enabled", true);
            exclusionGroups = builder.comment("格式: \"规则,词条1,词条2,...\"",
                            "规则 = \"roll\" (随机保留一个) 或 \"first\" (存在时保留第一个)")
                    .defineList("groups",
                            List.of("first,l2hostility:moonwalk,l2hostility:gravity"),
                            e -> e instanceof String s && s.contains(","));
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
                    catch (NumberFormatException e) {
                        valid = false;
                        break;
                    }
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
                if (!traits.isEmpty()) {
                    parsedExclusionGroups.add(new ExclusionGroup(rule, traits));
                }
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

    public record ExclusionGroup(String rule, List<String> traitIds) {}
}
