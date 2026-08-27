package com.l2hostility_tweaks.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitDisableHelperTest {

    private static final String TRAIT_ID = "l2hostility:split";
    private static final String SEALED_LEVEL_KEY = "l2htweaks_sealed_level_" + TRAIT_ID;

    @Test
    void snapshotsRemainingSealTicksForClientSynchronization() throws Exception {
        Method snapshotMethod = null;
        for (Method method : TraitDisableHelper.class.getDeclaredMethods()) {
            if (method.getName().equals("snapshotSealRemainingTicks")) {
                snapshotMethod = method;
                break;
            }
        }
        assertNotNull(snapshotMethod);

        CompoundTag data = new CompoundTag();
        data.putLong(TraitDisableHelper.sealExpiryKey("l2hostility:split"), 500L);
        data.putLong(TraitDisableHelper.sealExpiryKey("l2hostility:undying"), -1L);
        data.putInt(TraitDisableHelper.sealExpiryKey("l2hostility:invalid"), 700);
        data.putLong("unrelated", 999L);

        @SuppressWarnings("unchecked")
        Map<String, Long> snapshot = (Map<String, Long>) snapshotMethod.invoke(null, data, 380L);

        assertEquals(Map.of(
                "l2hostility:split", 120L,
                "l2hostility:undying", -1L), snapshot);
    }

    @Test
    void convertsSynchronizedSealTicksToAStableCountdown() throws Exception {
        Method countdownMethod = null;
        for (Method method : TraitDisableHelper.class.getDeclaredMethods()) {
            if (method.getName().equals("sealRemainingSeconds")) {
                countdownMethod = method;
                break;
            }
        }
        assertNotNull(countdownMethod);

        assertEquals(3L, countdownMethod.invoke(null, 41L, 0L));
        assertEquals(1L, countdownMethod.invoke(null, 41L, 21L));
        assertEquals(0L, countdownMethod.invoke(null, 41L, 80L));
        assertEquals(-1L, countdownMethod.invoke(null, -1L, 80L));
    }

    @Test
    void boundsSealSnapshotsToTheSharedNetworkLimit() {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i <= 1024; i++) {
            data.putLong(TraitDisableHelper.sealExpiryKey("example:trait_" + i), 500L + i);
        }

        Map<String, Long> snapshot = TraitDisableHelper.snapshotSealRemainingTicks(data, 100L);

        assertEquals(1024, snapshot.size());
    }

    @Test
    void playerSealCountdownUsesSideSafeNetworkSnapshots() throws Exception {
        String network = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java"));
        String screen = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java"));
        String proxy = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/proxy/IProxy.java"));
        String clientProxy = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/proxy/ClientProxy.java"));
        String helper = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/util/TraitDisableHelper.java"));
        String splitSuppressor = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/RemoveTraitEnchantmentMixin.java"));

        assertTrue(network.contains("record SealStateRequestPacket()"));
        assertTrue(network.contains("record SealStateSyncPacket(Map<String, Long> remainingTicks)"));
        assertTrue(network.contains("NetworkDirection.PLAY_TO_SERVER"));
        assertTrue(network.contains("NetworkDirection.PLAY_TO_CLIENT"));
        assertTrue(network.contains("TraitDisableHelper.MAX_SEAL_STATE_ENTRIES"));
        assertTrue(screen.contains("NetworkHandler.requestSealStateFromServer()"));
        assertFalse(screen.contains("getSingleplayerServer()"));
        assertFalse(screen.contains("getPersistentData()"));
        assertTrue(proxy.contains("receiveSealState(Map<String, Long> remainingTicks)"));
        assertTrue(clientProxy.contains("PlayerTraitScreen.receiveSealState(remainingTicks)"));
        assertTrue(helper.contains("NetworkHandler.sendSealStateToPlayer(player)"));
        assertTrue(splitSuppressor.indexOf("getPersistentData().putLong") <
                splitSuppressor.indexOf("TraitDisableHelper.setDisabled"));
    }

    @Test
    void rebuildsSealedLevelFromNegativeRawLevel() {
        CompoundTag data = new CompoundTag();

        TraitDisableHelper.syncSealedLevelData(data, TRAIT_ID, -3);

        assertEquals(3, data.getInt(SEALED_LEVEL_KEY));
    }

    @Test
    void removesStaleSealedLevelForPositiveRawLevel() {
        CompoundTag data = new CompoundTag();
        data.putInt(SEALED_LEVEL_KEY, 3);

        TraitDisableHelper.syncSealedLevelData(data, TRAIT_ID, 2);

        assertFalse(data.contains(SEALED_LEVEL_KEY));
    }

    @Test
    void clearsSealedLevelAndExpiryTogether() {
        CompoundTag data = new CompoundTag();
        String expiryKey = TraitDisableHelper.sealExpiryKey(TRAIT_ID);
        data.putInt(SEALED_LEVEL_KEY, 3);
        data.putLong(expiryKey, 1200L);

        TraitDisableHelper.clearSealData(data, TRAIT_ID);

        assertFalse(data.contains(SEALED_LEVEL_KEY));
        assertFalse(data.contains(expiryKey));
    }

    @Test
    void clearingUndyingSealDataAlsoClearsItsResurrectionCount() {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 2);

        TraitDisableHelper.clearSealData(data, TraitDisableHelper.UNDYING_TRAIT_ID);

        assertFalse(data.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void clearingAnotherTraitsSealDataKeepsUndyingResurrectionCount() {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 2);

        TraitDisableHelper.clearSealData(data, TRAIT_ID);

        assertEquals(2, data.getInt(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void clearsUndyingCountAfterUndyingIsUnsealed() {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 3);

        TraitDisableHelper.onTraitUnsealed(data, TraitDisableHelper.UNDYING_TRAIT_ID);

        assertFalse(data.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void keepsUndyingCountWhenAnotherTraitIsUnsealed() {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 3);

        TraitDisableHelper.onTraitUnsealed(data, TRAIT_ID);

        assertTrue(data.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
        assertEquals(3, data.getInt(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void hidesUndyingLimitDetailWhenUnlimitedOrSealingIsDisabled() {
        assertNull(TraitDisableHelper.buildUndyingLimitDetail(-1, 60));
        assertNull(TraitDisableHelper.buildUndyingLimitDetail(3, 0));
    }

    @Test
    void buildsTimedUndyingLimitDetail() {
        Component detail = TraitDisableHelper.buildUndyingLimitDetail(3, 60);

        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, detail.getContents());
        assertEquals("trait.l2hostility_tweaks.undying.limit_timed", contents.getKey());
        assertArrayEquals(new Object[]{3, 60}, contents.getArgs());
    }

    @Test
    void buildsPermanentUndyingLimitDetail() {
        Component detail = TraitDisableHelper.buildUndyingLimitDetail(3, -1);

        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, detail.getContents());
        assertEquals("trait.l2hostility_tweaks.undying.limit_permanent", contents.getKey());
        assertArrayEquals(new Object[]{3}, contents.getArgs());
    }

    @Test
    void detectsAnExhaustedUndyingLimitBeforeTheNextTrigger() throws Exception {
        Method limitMethod = null;
        for (Method method : TraitDisableHelper.class.getDeclaredMethods()) {
            if (method.getName().equals("isUndyingLimitExhausted")) {
                limitMethod = method;
                break;
            }
        }
        assertNotNull(limitMethod);

        assertEquals(true, limitMethod.invoke(null, 0, 0, 60));
        assertEquals(false, limitMethod.invoke(null, 3, 2, 60));
        assertEquals(true, limitMethod.invoke(null, 3, 3, 60));
        assertEquals(false, limitMethod.invoke(null, -1, 999, 60));
        assertEquals(false, limitMethod.invoke(null, 0, 0, 0));
    }

    @Test
    void undyingMixinChecksExhaustionBeforeCallingUpstreamResurrection() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/UndyingTraitMixin.java"));
        int headStart = source.indexOf("l2fix$limitResurrections");
        int tailStart = source.indexOf("l2fix$incrementCount");
        String headHandler = source.substring(headStart, tailStart);

        int headGuard = headHandler.indexOf("if (entity.level().isClientSide()) return;");
        int exhaustionCheck = headHandler.indexOf("TraitDisableHelper.isUndyingLimitExhausted");
        int sealCall = headHandler.indexOf("l2fix$sealUndying(entity, duration)");
        int cancelAfterSeal = headHandler.indexOf("ci.cancel();", sealCall);
        assertTrue(headGuard >= 0);
        assertTrue(headGuard < exhaustionCheck);
        assertTrue(exhaustionCheck < sealCall);
        assertTrue(sealCall < cancelAfterSeal);
        assertTrue(headHandler.contains("TraitDisableHelper.isUndyingLimitExhausted"));
        assertTrue(headHandler.contains("l2fix$sealUndying(entity, duration)"));

        int sealHelperStart = source.indexOf("private static void l2fix$sealUndying");
        String tailHandler = source.substring(tailStart, sealHelperStart);
        int tailGuard = tailHandler.indexOf("if (entity.level().isClientSide()) return;");
        int canceledCheck = tailHandler.indexOf("if (!event.isCanceled()) return;");
        assertTrue(tailGuard >= 0);
        assertTrue(tailGuard < canceledCheck);
        assertTrue(source.contains("private static void l2fix$sealUndying"));
    }

    @Test
    void snapshotsAndRestoresOnlyManagedRuntimeState() {
        String firstExpiry = TraitDisableHelper.sealExpiryKey("l2hostility:undying");
        String secondExpiry = TraitDisableHelper.sealExpiryKey("l2hostility:split");
        CompoundTag source = new CompoundTag();
        source.putLong(firstExpiry, 1200L);
        source.putLong(secondExpiry, -1L);
        source.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 2);
        source.putInt("l2htweaks_sealed_level_l2hostility:undying", 1);
        source.putString("unrelated", "value");

        CompoundTag snapshot = TraitDisableHelper.snapshotRuntimeState(source);
        source.putLong(firstExpiry, 9999L);
        CompoundTag target = new CompoundTag();
        TraitDisableHelper.restoreRuntimeState(target, snapshot);

        assertEquals(1200L, target.getLong(firstExpiry));
        assertEquals(-1L, target.getLong(secondExpiry));
        assertEquals(2, target.getInt(TraitDisableHelper.UNDYING_COUNT_KEY));
        assertFalse(target.contains("l2htweaks_sealed_level_l2hostility:undying"));
        assertFalse(target.contains("unrelated"));
        target.putLong(firstExpiry, 7777L);
        assertEquals(1200L, snapshot.getLong(firstExpiry));
    }

    @Test
    void keepsUndyingCountAbsentWhenSourceDoesNotContainIt() {
        CompoundTag source = new CompoundTag();
        source.putLong(TraitDisableHelper.sealExpiryKey("l2hostility:split"), 400L);

        CompoundTag snapshot = TraitDisableHelper.snapshotRuntimeState(source);
        CompoundTag target = new CompoundTag();
        TraitDisableHelper.restoreRuntimeState(target, snapshot);

        assertFalse(target.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void resetsEveryInheritedTraitBeforeClearingCapabilityState() {
        var traits = new LinkedHashMap<String, Integer>();
        traits.put("first", 2);
        traits.put("second", -1);
        var resets = new ArrayList<String>();
        var runtimeClears = new ArrayList<String>();

        TraitDisableHelper.clearTraitState(traits, trait -> {
            assertTrue(traits.containsKey(trait));
            resets.add(trait);
        }, runtimeClears::add);

        assertTrue(traits.isEmpty());
        assertEquals(List.of("first", "second"), resets);
        assertEquals(List.of("first", "second"), runtimeClears);
    }

    @Test
    void keepsDisplayEntityApiButRemovesInternalHotPath() throws Exception {
        var setApi = TraitDisableHelper.class.getDeclaredMethod("setDisplayEntity", LivingEntity.class);
        var clearApi = TraitDisableHelper.class.getDeclaredMethod("clearDisplayEntity");
        var getApi = TraitDisableHelper.class.getDeclaredMethod("getDisplayEntity");
        assertTrue(Modifier.isPublic(setApi.getModifiers()) && Modifier.isStatic(setApi.getModifiers()));
        assertTrue(Modifier.isPublic(clearApi.getModifiers()) && Modifier.isStatic(clearApi.getModifiers()));
        assertTrue(Modifier.isPublic(getApi.getModifiers()) && Modifier.isStatic(getApi.getModifiers()));
        assertNotNull(TraitDisableHelper.class.getDeclaredField("DISPLAY_ENTITY"));

        Path clientMixin = Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/ClientEventsMixin.java");
        assertFalse(Files.exists(clientMixin));

        String mixinConfig = Files.readString(Path.of(
                "src/main/resources/l2hostility_tweaks.mixins.json"));
        String overlay = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/L2HHealthOverlay.java"));
        String screen = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/PlayerTraitScreen.java"));
        assertFalse(mixinConfig.contains("ClientEventsMixin"));
        assertFalse(overlay.contains("setDisplayEntity"));
        assertFalse(overlay.contains("clearDisplayEntity"));
        assertFalse(screen.contains("setDisplayEntity"));
        assertFalse(screen.contains("clearDisplayEntity"));
        assertTrue(screen.contains("setHideRealityDetail"));
    }

    @Test
    void legacySealedLevelWithoutExpiryBecomesATrackedPermanentSeal() throws Exception {
        CompoundTag data = new CompoundTag();
        data.putInt(SEALED_LEVEL_KEY, 3);

        Set<String> expired = reconcileSealData(data, Map.of(TRAIT_ID, -3), 200L);

        assertTrue(expired.isEmpty());
        assertEquals(-1L, data.getLong(TraitDisableHelper.sealExpiryKey(TRAIT_ID)));
        assertTrue(data.getBoolean("l2htweaks_has_seal_state"));
    }

    @Test
    void staleSealDataIsClearedWhenTheCurrentTraitLevelIsPositive() throws Exception {
        CompoundTag data = new CompoundTag();
        data.putInt(SEALED_LEVEL_KEY, 3);
        data.putLong(TraitDisableHelper.sealExpiryKey(TRAIT_ID), 500L);

        reconcileSealData(data, Map.of(TRAIT_ID, 3), 200L);

        assertFalse(data.contains(SEALED_LEVEL_KEY));
        assertFalse(data.contains(TraitDisableHelper.sealExpiryKey(TRAIT_ID)));
        assertFalse(data.contains("l2htweaks_has_seal_state"));
    }

    @Test
    void expiredNegativeTraitIsReturnedForAuthoritativeUnsealing() throws Exception {
        CompoundTag data = new CompoundTag();
        data.putInt(SEALED_LEVEL_KEY, 3);
        data.putLong(TraitDisableHelper.sealExpiryKey(TRAIT_ID), 150L);

        Set<String> expired = reconcileSealData(data, Map.of(TRAIT_ID, -3), 200L);

        assertEquals(Set.of(TRAIT_ID), expired);
        assertTrue(data.contains(SEALED_LEVEL_KEY));
    }

    @Test
    void legacyUndyingCountIsRemovedWhenUndyingNoLongerExists() throws Exception {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 4);

        reconcileSealData(data, Map.of(), 200L);

        assertFalse(data.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
    }

    @Test
    void activeUndyingCountKeepsMaintenanceMarkerUntilTheTraitDisappears() throws Exception {
        CompoundTag data = new CompoundTag();
        data.putInt(TraitDisableHelper.UNDYING_COUNT_KEY, 2);

        reconcileSealData(data, Map.of(TraitDisableHelper.UNDYING_TRAIT_ID, 1), 200L);

        assertTrue(data.getBoolean("l2htweaks_has_seal_state"));
        assertEquals(2, data.getInt(TraitDisableHelper.UNDYING_COUNT_KEY));

        reconcileSealData(data, Map.of(), 220L);

        assertFalse(data.contains(TraitDisableHelper.UNDYING_COUNT_KEY));
        assertFalse(data.contains("l2htweaks_has_seal_state"));
    }

    @Test
    void sealedLevelWritesMaintainTheFastPathMarker() {
        CompoundTag data = new CompoundTag();

        TraitDisableHelper.syncSealedLevelData(data, TRAIT_ID, -2);
        assertTrue(data.getBoolean("l2htweaks_has_seal_state"));

        TraitDisableHelper.syncSealedLevelData(data, TRAIT_ID, 2);
        assertFalse(data.contains("l2htweaks_has_seal_state"));
    }

    @Test
    void clearingTheLastSealRemovesTheFastPathMarker() {
        CompoundTag data = new CompoundTag();
        data.putBoolean("l2htweaks_has_seal_state", true);
        data.putInt(SEALED_LEVEL_KEY, 2);
        data.putLong(TraitDisableHelper.sealExpiryKey(TRAIT_ID), -1L);

        TraitDisableHelper.clearSealData(data, TRAIT_ID);

        assertFalse(data.contains("l2htweaks_has_seal_state"));
    }

    @Test
    void capabilityTickOwnsSealMaintenanceWithoutAGlobalLivingTickScan() throws Exception {
        String main = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
        String mixin = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/MobTraitCapTickMixin.java"));
        String undyingMixin = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/UndyingTraitMixin.java"));

        assertFalse(main.contains("LivingEvent.LivingTickEvent"));
        assertTrue(mixin.contains("private boolean l2fix$sealStateInitialized"));
        assertTrue(mixin.contains("@Inject(method = \"tick\", at = @At(\"TAIL\")"));
        assertTrue(mixin.contains("TraitDisableHelper.hasSealStateMarker"));
        assertTrue(mixin.contains("TraitDisableHelper.maintainSealState"));
        assertTrue(mixin.contains("(mob.tickCount + mob.getId()) % 20 != 0"));
        assertTrue(mixin.indexOf("(mob.tickCount + mob.getId()) % 20 != 0")
                < mixin.indexOf("TraitDisableHelper.hasSealStateMarker"));
        assertTrue(undyingMixin.contains("TraitDisableHelper.syncUndyingCountData(pd, count)"));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> reconcileSealData(CompoundTag data, Map<String, Integer> levels,
                                                  long gameTime) throws Exception {
        Method method = null;
        for (Method candidate : TraitDisableHelper.class.getDeclaredMethods()) {
            if (candidate.getName().equals("reconcileSealData")) {
                method = candidate;
                break;
            }
        }
        assertNotNull(method);
        return (Set<String>) method.invoke(null, data, levels, gameTime);
    }

    @Test
    void deathRecoveryUsesDebugForNormalDiagnosticsAndWarnForFailures() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));
        int deathFlowStart = source.indexOf("public void onLivingDeath");
        int deathFlowEnd = source.indexOf("public void onBreakSpeed", deathFlowStart);
        String deathFlow = source.substring(deathFlowStart, deathFlowEnd);

        assertFalse(deathFlow.contains("LOGGER.info("));
        assertTrue(deathFlow.contains("LOGGER.debug(\"DEATH:"));
        assertTrue(deathFlow.contains("LOGGER.debug(\"CLONE:"));
        assertTrue(deathFlow.contains("LOGGER.debug(\"SYNC:"));
        assertTrue(deathFlow.contains("LOGGER.warn(\"CLONE: no death snapshot"));
        assertTrue(deathFlow.contains("LOGGER.warn(\"CLONE: HOLDER.isProper failed"));
    }
}
