package com.l2hostility_tweaks.config;

import com.l2hostility_tweaks.network.NetworkHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class L2HConfigTest {

    @Test
    void exponentialCostDescriptionMatchesRuntimeSchedule() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/config/L2HConfig.java"));

        assertTrue(source.contains(
                "3 = 指数: 消耗 2^当前等级 个（依次为 1、2、4、8……）"));
        assertFalse(source.contains("2^(当前等级 - 1)"));
    }

    @Test
    void traitCostConfigContainsNoDirectBitShift() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/config/L2HConfig.java"));

        assertFalse(source.contains("1 <<"));
    }

    @Test
    void invalidatesEveryParsedCommonConfigCache() throws Exception {
        List<String> cacheNames = List.of(
                "parsedLevelThresholds",
                "parsedPerTraitThresholds",
                "parsedLegendaryThresholds",
                "parsedExtraLegendaryIds",
                "parsedExclusionGroups",
                "parsedSealDurationArray",
                "parsedPlayerTraitOverrides");

        for (String cacheName : cacheNames) {
            Field field = L2HConfig.class.getDeclaredField(cacheName);
            field.setAccessible(true);
            Object sentinel = Map.class.isAssignableFrom(field.getType())
                    ? new LinkedHashMap<>()
                    : Set.class.isAssignableFrom(field.getType())
                    ? new LinkedHashSet<>()
                    : new ArrayList<>();
            field.set(null, sentinel);
        }

        L2HConfig.invalidateCaches();

        for (String cacheName : cacheNames) {
            Field field = L2HConfig.class.getDeclaredField(cacheName);
            field.setAccessible(true);
            assertNull(field.get(null), cacheName);
        }
    }

    @Test
    void filtersInvalidExclusionIdsButKeepsUnregisteredIds() {
        var groups = L2HConfig.parseExclusionGroups(List.of(
                "first,l2hostility:gravity,Invalid Trait ID,addon:future_trait"));

        assertEquals(1, groups.size());
        assertEquals("first", groups.get(0).rule());
        assertEquals(List.of("l2hostility:gravity", "addon:future_trait"), groups.get(0).traitIds());
    }

    @Test
    void killerAuraIntervalsRequirePositiveIntegers() {
        assertTrue(L2HConfig.isPositiveInteger(1));
        assertFalse(L2HConfig.isPositiveInteger(0));
        assertFalse(L2HConfig.isPositiveInteger(-1));
        assertFalse(L2HConfig.isPositiveInteger("1"));
    }

    @Test
    void killerAuraIntervalNeverFallsBelowOneTick() {
        assertEquals(20, L2HConfig.sanitizeKillerAuraInterval(20));
        assertEquals(1, L2HConfig.sanitizeKillerAuraInterval(0));
        assertEquals(1, L2HConfig.sanitizeKillerAuraInterval(-20));
    }

    @Test
    void remoteDisplaySnapshotOverridesGameplayValuesWithoutAliasing() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putDouble("antiReprintReduction", 0.37);
        snapshot.putIntArray("ragnarokCountArray", new int[]{3, 7});
        snapshot.putIntArray("ragnarokTimeArray", new int[]{200, 500});
        snapshot.putIntArray("killerAuraDamageArray", new int[]{11, 23});
        snapshot.putIntArray("killerAuraIntervalArray", new int[]{40, 10});
        snapshot.putInt("killerAuraRange", 17);
        snapshot.putInt("bottleOfCurseLevel", 41);
        snapshot.putIntArray("drainDamageArray", new int[]{25, 50});
        snapshot.putIntArray("drainDurationArray", new int[]{30, 60});
        snapshot.putIntArray("drainDurationMaxArray", new int[]{8, 12});
        snapshot.putIntArray("drainCountArray", new int[]{1, 4});

        try {
            L2HConfig.installDisplaySnapshot(snapshot);
            snapshot.putDouble("antiReprintReduction", 0.99);

            assertTrue(L2HConfig.hasDisplaySnapshot());
            assertEquals(0.37, L2HConfig.getDisplayAntiReprintReduction());
            assertEquals(7, L2HConfig.getDisplayRagnarokCount(3));
            assertEquals(600, L2HConfig.getDisplayRagnarokTime(3));
            assertEquals(23, L2HConfig.getDisplayKillerAuraDamage(2));
            assertEquals(10, L2HConfig.getDisplayKillerAuraInterval(2));
            assertEquals(17, L2HConfig.getDisplayKillerAuraRange());
            assertEquals(41, L2HConfig.getDisplayBottleOfCurseLevel());
            assertEquals(0.5, L2HConfig.getDisplayDrainDamage(2));
            assertEquals(0.6, L2HConfig.getDisplayDrainDuration(2));
            assertEquals(240, L2HConfig.getDisplayDrainDurationMax(2));
            assertEquals(4, L2HConfig.getDisplayDrainCount(2));
        } finally {
            L2HConfig.clearDisplaySnapshot();
        }

        assertFalse(L2HConfig.hasDisplaySnapshot());
    }

    @Test
    void remoteDisplaySnapshotSuppliesTraitMetadata() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putBoolean("exclusionEnabled", true);
        snapshot.putBoolean("playerSelfTraitBalanceEnabled", true);
        snapshot.putDouble("playerSelfTraitBudgetRatio", 2.5);
        snapshot.putInt("playerSelfTraitCostMode", 3);
        snapshot.put("extraLegendaryIds", stringList("addon:legend"));
        snapshot.put("exclusionGroups", stringList("pair,addon:first,addon:second"));
        snapshot.put("playerTraitOverrides", stringList("addon:first,120,9"));

        try {
            L2HConfig.installDisplaySnapshot(snapshot);

            assertEquals(Set.of("addon:legend"), L2HConfig.getDisplayExtraLegendaryIds());
            assertTrue(L2HConfig.isDisplayExclusionEnabled());
            assertEquals(List.of("addon:first", "addon:second"),
                    L2HConfig.getDisplayExclusionGroups().get(0).traitIds());
            assertTrue(L2HConfig.isDisplayPlayerSelfTraitBalanceEnabled());
            assertEquals(2.5, L2HConfig.getDisplayPlayerSelfTraitBudgetRatio());
            assertEquals(4, L2HConfig.getDisplayUpgradeCost(2, 64));
            assertEquals(new L2HConfig.PlayerTraitOverride(120, 9),
                    L2HConfig.getDisplayPlayerTraitOverrides().get("addon:first"));
        } finally {
            L2HConfig.clearDisplaySnapshot();
        }
    }

    @Test
    void remoteDisplaySnapshotSuppliesEveryAdditionalClientRule() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putBoolean("reprintLinearEnabled", true);
        snapshot.putDouble("reprintDamageFactor", 0.14);
        snapshot.putBoolean("adaptiveLinearEnabled", true);
        snapshot.putDouble("adaptiveReductionPerStack", 0.08);
        snapshot.putDouble("adaptiveMaxReduction", 0.64);
        snapshot.putBoolean("detectorGlassesReveal", false);
        snapshot.putInt("detectorGlassesRange", 29);
        snapshot.putBoolean("oldDispell", true);
        snapshot.putBoolean("oldDementor", true);
        snapshot.putInt("undyingMaxResurrections", 5);
        snapshot.putInt("undyingSealDuration", 300);
        snapshot.putIntArray("dispellTimeArray", new int[]{200, 500});
        snapshot.putInt("dispellBaseTime", 120);
        snapshot.putIntArray("dispellCountArray", new int[]{2, 4});
        snapshot.putBoolean("levelCapEnabled", true);
        snapshot.putInt("levelCapUnlimited", 900);
        snapshot.put("levelCapThresholds", stringList("100,2", "300,4"));
        snapshot.putBoolean("legendaryEnabled", true);
        snapshot.putInt("legendaryUnlimited", 1200);
        snapshot.put("legendaryThresholds", stringList("200,1", "500,2"));

        try {
            L2HConfig.installDisplaySnapshot(snapshot);

            assertTrue(L2HConfig.isDisplayReprintLinearEnabled());
            assertEquals(0.14, L2HConfig.getDisplayReprintDamage());
            assertTrue(L2HConfig.isDisplayAdaptiveLinearEnabled());
            assertEquals(0.08, L2HConfig.getDisplayAdaptiveReductionPerStack());
            assertEquals(0.64, L2HConfig.getDisplayAdaptiveMaxReduction());
            assertFalse(L2HConfig.isDisplayDetectorGlassesRevealEnabled());
            assertEquals(29, L2HConfig.getDisplayDetectorGlassesRange());
            assertTrue(L2HConfig.isDisplayOldDispellEnabled());
            assertTrue(L2HConfig.isDisplayOldDementorEnabled());
            assertEquals(5, L2HConfig.getDisplayUndyingMaxResurrections());
            assertEquals(300, L2HConfig.getDisplayUndyingSealDuration());
            assertEquals(4, L2HConfig.getDisplayDispellCount(3));
            assertEquals(500, L2HConfig.getDisplayDispellTime(2));
            assertTrue(L2HConfig.isDisplayLevelCapEnabled());
            assertEquals(900, L2HConfig.getDisplayLevelCapUnlimited());
            assertEquals(4, L2HConfig.getThreshold(L2HConfig.getDisplayLevelThresholds(), 350));
            List<int[]> exposedThresholds = L2HConfig.getDisplayLevelThresholds();
            exposedThresholds.get(0)[1] = 99;
            assertEquals(2, L2HConfig.getDisplayLevelThresholds().get(0)[1]);
            assertTrue(L2HConfig.isDisplayLegendaryEnabled());
            assertEquals(1200, L2HConfig.getDisplayLegendaryUnlimited());
            assertEquals(2, L2HConfig.getThreshold(L2HConfig.getDisplayLegendaryThresholds(), 600));
        } finally {
            L2HConfig.clearDisplaySnapshot();
        }
    }

    @Test
    void remoteDisplaySnapshotSuppliesSealTraitDurations() throws Exception {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("sealDurationMode", 2);
        snapshot.putInt("sealDurationLinear", 7);
        snapshot.putIntArray("sealDurationArray", new int[]{5, 12});

        try {
            L2HConfig.installDisplaySnapshot(snapshot);

            assertEquals(5, displaySealDurationSeconds(1));
            assertEquals(12, displaySealDurationSeconds(2));
            assertEquals(19, displaySealDurationSeconds(3));

            snapshot.putInt("sealDurationMode", 1);
            L2HConfig.installDisplaySnapshot(snapshot);
            assertEquals(21, displaySealDurationSeconds(3));

            snapshot.putInt("sealDurationMode", 2);
            snapshot.putIntArray("sealDurationArray", new int[0]);
            L2HConfig.installDisplaySnapshot(snapshot);
            assertEquals(21, displaySealDurationSeconds(3));
        } finally {
            L2HConfig.clearDisplaySnapshot();
        }
    }

    @Test
    void serverDisplaySnapshotContainsSealTraitTimingConfiguration() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/config/L2HConfig.java"));

        assertTrue(source.contains("tag.putInt(\"sealDurationMode\""));
        assertTrue(source.contains("tag.putInt(\"sealDurationLinear\""));
        assertTrue(source.contains("putIntList(tag, \"sealDurationArray\""));
    }

    @Test
    void sealTraitUsesServerTimingForGameplayAndDisplayTimingForTooltip() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/content/traits/SealTrait.java"));

        assertTrue(source.contains("L2HConfig.getDisplaySealDurationSeconds(i)"));
        assertTrue(source.contains("L2HConfig.getSealDurationSeconds(level)"));
    }

    @Test
    void remoteDisplaySnapshotUsesServerFallbacksWhenArraysAreEmpty() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putIntArray("ragnarokCountArray", new int[0]);
        snapshot.putIntArray("ragnarokTimeArray", new int[0]);
        snapshot.putInt("ragnarokBaseTime", 120);
        snapshot.putIntArray("killerAuraDamageArray", new int[0]);
        snapshot.putInt("killerAuraBaseDamage", 9);
        snapshot.putIntArray("killerAuraIntervalArray", new int[0]);
        snapshot.putInt("killerAuraBaseInterval", 40);
        snapshot.putIntArray("drainDamageArray", new int[0]);
        snapshot.putDouble("drainBaseDamage", 0.15);
        snapshot.putIntArray("drainDurationArray", new int[0]);
        snapshot.putDouble("drainBaseDuration", 0.2);
        snapshot.putIntArray("drainDurationMaxArray", new int[0]);
        snapshot.putInt("drainBaseDurationMax", 100);
        snapshot.putIntArray("drainCountArray", new int[0]);

        try {
            L2HConfig.installDisplaySnapshot(snapshot);

            assertEquals(3, L2HConfig.getDisplayRagnarokCount(3));
            assertEquals(360, L2HConfig.getDisplayRagnarokTime(3));
            assertEquals(27, L2HConfig.getDisplayKillerAuraDamage(3));
            assertEquals(13, L2HConfig.getDisplayKillerAuraInterval(3));
            assertEquals(0.45, L2HConfig.getDisplayDrainDamage(3), 1.0E-9);
            assertEquals(0.6, L2HConfig.getDisplayDrainDuration(3), 1.0E-9);
            assertEquals(300, L2HConfig.getDisplayDrainDurationMax(3));
            assertEquals(3, L2HConfig.getDisplayDrainCount(3));
        } finally {
            L2HConfig.clearDisplaySnapshot();
        }
    }

    @Test
    void rejectsOversizedDisplaySnapshotCollections() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putIntArray("ragnarokCountArray",
                new int[L2HConfig.MAX_DISPLAY_CONFIG_ENTRIES + 1]);

        assertThrows(IllegalArgumentException.class,
                () -> L2HConfig.installDisplaySnapshot(snapshot));
    }

    @Test
    void rejectsOversizedSealDurationDisplayArray() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putIntArray("sealDurationArray",
                new int[L2HConfig.MAX_DISPLAY_CONFIG_ENTRIES + 1]);

        assertThrows(IllegalArgumentException.class,
                () -> L2HConfig.installDisplaySnapshot(snapshot));
    }

    @Test
    void rejectsOversizedDisplaySnapshotStrings() {
        CompoundTag snapshot = new CompoundTag();
        snapshot.put("extraLegendaryIds", stringList("x".repeat(
                L2HConfig.MAX_DISPLAY_CONFIG_STRING_LENGTH + 1)));

        assertThrows(IllegalArgumentException.class,
                () -> L2HConfig.installDisplaySnapshot(snapshot));
    }

    @Test
    void rejectsExcessiveCombinedDisplaySnapshotStrings() {
        CompoundTag snapshot = new CompoundTag();
        ListTag values = new ListTag();
        String value = "x".repeat(L2HConfig.MAX_DISPLAY_CONFIG_STRING_LENGTH);
        int count = L2HConfig.MAX_DISPLAY_CONFIG_TOTAL_STRING_LENGTH
                / L2HConfig.MAX_DISPLAY_CONFIG_STRING_LENGTH + 1;
        for (int i = 0; i < count; i++) values.add(StringTag.valueOf(value));
        snapshot.put("extraLegendaryIds", values);

        assertThrows(IllegalArgumentException.class,
                () -> L2HConfig.installDisplaySnapshot(snapshot));
    }

    @Test
    void displayConfigPacketRoundTripsWithDefensiveCopies() {
        CompoundTag original = new CompoundTag();
        original.putDouble("antiReprintReduction", 0.37);
        original.putIntArray("killerAuraDamageArray", new int[]{11, 23});
        NetworkHandler.DisplayConfigSyncPacket packet =
                new NetworkHandler.DisplayConfigSyncPacket(original);
        original.putDouble("antiReprintReduction", 0.99);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        NetworkHandler.DisplayConfigSyncPacket.encode(packet, buffer);
        NetworkHandler.DisplayConfigSyncPacket decoded =
                NetworkHandler.DisplayConfigSyncPacket.decode(buffer);

        CompoundTag exposed = decoded.values();
        assertEquals(0.37, exposed.getDouble("antiReprintReduction"));
        assertEquals(List.of(11, 23), Arrays.stream(
                exposed.getIntArray("killerAuraDamageArray")).boxed().toList());
        exposed.putDouble("antiReprintReduction", 0.75);
        assertEquals(0.37, decoded.values().getDouble("antiReprintReduction"));
    }

    private static ListTag stringList(String... values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        return list;
    }

    private static int displaySealDurationSeconds(int level) throws Exception {
        Method method = Arrays.stream(L2HConfig.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("getDisplaySealDurationSeconds"))
                .findFirst().orElse(null);
        assertNotNull(method);
        return (int) method.invoke(null, level);
    }
}
