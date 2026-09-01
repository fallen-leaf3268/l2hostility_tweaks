package com.l2hostility_tweaks.client.config;

import net.minecraft.util.FastColor;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClientL2HConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    private static volatile List<int[]> parsedColorSegments;

    static {
        Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    public static class Client {

        public final ForgeConfigSpec.BooleanValue showHud;
        public final ForgeConfigSpec.IntValue hudXOffset;
        public final ForgeConfigSpec.IntValue hudYOffset;
        public final ForgeConfigSpec.IntValue hudRange;
        public final ForgeConfigSpec.BooleanValue hideHudWithBossbar;
        public final ForgeConfigSpec.BooleanValue romanNumerals;
        public final ForgeConfigSpec.DoubleValue gradientStrength;
        public final ForgeConfigSpec.IntValue hudBarWidth;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> colorSegments;
        public final ForgeConfigSpec.ConfigValue<String> defaultColor;

        Client(ForgeConfigSpec.Builder builder) {
            builder.push("hud");

            showHud = builder.comment("显示自定义血条 HUD")
                    .define("enabled", false);

            hudXOffset = builder.comment("HUD 水平偏移")
                    .defineInRange("hudXOffset", 0, -1000, 1000);

            hudYOffset = builder.comment("HUD 垂直偏移")
                    .defineInRange("hudYOffset", 0, -1000, 1000);

            hudRange = builder.comment("HUD 显示距离 (格)")
                    .defineInRange("hudRange", 50, 0, 1000);

            hideHudWithBossbar = builder.comment("有原版 BossBar 时隐藏自定义血条 (仍显示词条)")
                    .define("hideHudWithBossbar", false);

            romanNumerals = builder.comment("词条等级使用罗马数字 (I, II, III...)")
                    .define("romanNumerals", false);

            hudBarWidth = builder.comment("血条宽度 (像素)")
                    .defineInRange("hudBarWidth", 256, 100, 400);

            builder.pop();

            builder.push("color");
            gradientStrength = builder.comment("血条两端渐变暗度 (0=纯色, 1=最暗)",
                            "0.0 ~ 1.0")
                    .defineInRange("gradientStrength", 0.6, 0.0, 1.0);

            colorSegments = builder.comment("血条颜色分段",
                            "格式: \"难度,R,G,B\"",
                            "RGB 范围: 0–255",
                            "例: \"100,85,255,85\" = 难度 >= 100 时使用颜色 RGB(85,255,85)")
                    .defineList("segments",
                            List.of("100,85,255,85",
                                    "250,170,255,85",
                                    "500,255,200,60",
                                    "1000,255,100,70",
                                    "2000,255,60,60",
                                    "3000,200,40,120",
                                    "4000,160,30,180",
                                    "5000,100,20,200"),
                            e -> ClientL2HConfig.parseColorSegment(e) != null);
            defaultColor = builder.comment("默认血条颜色 (不匹配任何分段时使用)",
                            "格式: \"R,G,B\"",
                            "RGB 范围: 0–255")
                    .define("default_color", "170,170,170",
                            e -> ClientL2HConfig.parseRgb(e) != null);
            builder.pop();
        }
    }

    static int[] parseColorSegment(Object value) {
        return parseUnsignedFields(value, 4, 1);
    }

    static int[] parseRgb(Object value) {
        return parseUnsignedFields(value, 3, 0);
    }

    private static int[] parseUnsignedFields(Object value, int fieldCount, int rgbStart) {
        if (!(value instanceof String text)) return null;
        String[] parts = text.split(",", -1);
        if (parts.length != fieldCount) return null;
        int[] parsed = new int[fieldCount];
        try {
            for (int i = 0; i < fieldCount; i++) {
                String part = parts[i].trim();
                if (!part.matches("\\d+")) return null;
                parsed[i] = Integer.parseInt(part);
                if (i >= rgbStart && parsed[i] > 255) return null;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return parsed;
    }

    public static List<int[]> getColorSegments() {
        if (parsedColorSegments == null) {
            parsedColorSegments = parseColorSegments(CLIENT.colorSegments.get());
        }
        return parsedColorSegments;
    }

    static List<int[]> parseColorSegments(List<? extends String> entries) {
        List<int[]> parsed = new ArrayList<>();
        for (String entry : entries) {
            int[] segment = parseColorSegment(entry);
            if (segment != null) parsed.add(segment);
        }
        parsed.sort(Comparator.comparingInt(a -> a[0]));
        return parsed;
    }

    static int parseDefaultColor(Object value) {
        int[] rgb = parseRgb(value);
        if (rgb != null) return FastColor.ARGB32.color(255, rgb[0], rgb[1], rgb[2]);
        return FastColor.ARGB32.color(255, 170, 170, 170);
    }

    public static int getDefaultColor() {
        return parseDefaultColor(CLIENT.defaultColor.get());
    }

    public static void invalidateCaches() {
        parsedColorSegments = null;
    }
}
