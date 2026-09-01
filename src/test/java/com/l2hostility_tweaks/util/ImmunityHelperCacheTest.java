package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmunityHelperCacheTest {

    @AfterEach
    void clearCaches() {
        ImmunityHelper.invalidateTagCaches();
    }

    @Test
    void resolvesValueAfterDiscoveryRuns() {
        AtomicReference<String> value = new AtomicReference<>();

        String result = ImmunityHelper.resolveAfterDiscovery(
                () -> value.set("ready"), value::get);

        assertEquals("ready", result);
    }

    @Test
    void cacheStampPreservesGenerationAndSignedTickBits() {
        assertEquals(0x00000007ffffffffL, ImmunityHelper.cacheStamp(7, -1));
        assertEquals(0x8000000080000000L,
                ImmunityHelper.cacheStamp(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    @Test
    void generationUsesAtomicReloadUpdates() throws ReflectiveOperationException, IOException {
        Field generation = field("immunityCacheGeneration");
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java"));

        assertEquals(AtomicInteger.class, generation.getType());
        assertTrue(Modifier.isFinal(generation.getModifiers()));
        assertTrue(source.contains("immunityCacheGeneration.incrementAndGet()"));
    }

    @Test
    void invalidationClearsTraitCacheAndAdvancesEntityGeneration() throws ReflectiveOperationException {
        Map<String, Boolean> traitCache = traitCache();
        traitCache.put("l2hostility:test:l2hostility_tweaks:immune_to_force", true);
        int beforeGeneration = generation().get();
        long beforeStamp = ImmunityHelper.cacheStamp(beforeGeneration, 20);

        ImmunityHelper.invalidateTagCaches();

        assertTrue(traitCache.isEmpty());
        int afterGeneration = generation().get();
        assertEquals(beforeGeneration + 1, afterGeneration);
        assertNotEquals(beforeStamp, ImmunityHelper.cacheStamp(afterGeneration, 20));
    }

    @Test
    void runtimeUsesRegisteredEntityInstanceCache() throws IOException {
        String helper = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java"));
        String mixin = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/LivingEntityImmunityCacheMixin.java"));
        String config = Files.readString(Path.of(
                "src/main/resources/l2hostility_tweaks.mixins.json"));

        assertTrue(helper.contains("((EntityImmunityCache) entity).l2fix$isImmuneToForce(stamp)"));
        assertTrue(helper.contains("((EntityImmunityCache) entity).l2fix$isImmuneToGravity(stamp)"));
        assertTrue(config.contains("\"LivingEntityImmunityCacheMixin\""));
        assertTrue(mixin.indexOf("l2fix$forceImmunity = l2fix$scanForceImmunity()")
                < mixin.indexOf("l2fix$forceImmunityStamp = stamp"));
        assertTrue(mixin.indexOf("l2fix$gravityImmunity = l2fix$scanGravityImmunity()")
                < mixin.indexOf("l2fix$gravityImmunityStamp = stamp"));

        for (String removed : new String[]{
                "cachedEntityForceRef", "cacheTickForce", "cachedImmuneToForce",
                "cachedEntityGravityRef", "cacheTickGravity", "cachedImmuneToGravity",
                "WeakReference"}) {
            assertFalse(helper.contains(removed), removed);
        }
    }

    @Test
    void combatCurioQueriesShareOneEntitySnapshotAndInvalidateOnEquipmentChange() throws IOException {
        String helper = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java"));
        String ringListener = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/content/RingDamageListener.java"));
        String adapting = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/AdaptingTraitMixin.java"));
        String dementor = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DementorTraitMixin.java"));
        String traitImmunity = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/MobTraitImmunityMixin.java"));
        String main = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/L2HostilityFix.java"));

        assertTrue(helper.contains("getCombatCurios(LivingEntity entity)"));
        assertTrue(ringListener.contains("ImmunityHelper.getCombatCurios(attacker)"));
        assertFalse(ringListener.contains("CuriosApi.getCuriosInventory"));
        assertTrue(adapting.contains("ImmunityHelper.hasCombatCurioWithTag"));
        assertTrue(dementor.contains("ImmunityHelper.hasCombatCurioWithTag"));
        assertTrue(traitImmunity.contains("ImmunityHelper.hasCombatCurioWithTag"));
        assertTrue(main.contains("CurioChangeEvent"));
        assertTrue(main.contains("ImmunityHelper.invalidateCombatCurios(event.getEntity())"));
    }

    @Test
    void ringBypassUsesTheSameIndirectAttackerResolutionAsAttackCache() throws IOException {
        String helper = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java"));
        String attackCache = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/AttackCacheMixin.java"));
        String adapting = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/AdaptingTraitMixin.java"));
        String dementor = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DementorTraitMixin.java"));
        String traitImmunity = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/MobTraitImmunityMixin.java"));

        assertTrue(helper.contains("resolveLivingAttacker(DamageSource source)"));
        assertTrue(helper.contains("source.getDirectEntity()"));
        assertTrue(helper.contains("direct instanceof Projectile"));
        assertTrue(attackCache.contains("ImmunityHelper.resolveLivingAttacker(source)"));
        assertTrue(adapting.contains("ImmunityHelper.resolveLivingAttacker(event.getSource())"));
        assertFalse(adapting.contains("cache.getAttacker()"));
        assertTrue(dementor.contains("ImmunityHelper.resolveLivingAttacker(event.getSource())"));
        assertFalse(dementor.contains("cache.getAttacker()"));
        assertTrue(traitImmunity.contains("ImmunityHelper.resolveLivingAttacker(source)"));
    }

    @Test
    void adaptiveMixinTargetsTheDeclaredLivingHurtHook() throws IOException {
        String adapting = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/AdaptingTraitMixin.java"));

        assertTrue(adapting.contains("@Inject(method = \"onHurtByOthers\""));
        assertTrue(adapting.contains("LivingHurtEvent event"));
        assertFalse(adapting.contains("@Inject(method = \"onDamaged\""));
        assertFalse(adapting.contains("AttackCache cache"));
    }

    @Test
    void dementorMixinTargetsTheDeclaredLivingAttackHook() throws IOException {
        String dementor = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/DementorTraitMixin.java"));

        assertTrue(dementor.contains("@Inject(method = \"onAttackedByOthers\""));
        assertTrue(dementor.contains("LivingAttackEvent event"));
        assertTrue(dementor.contains("event.setCanceled(true)"));
        assertTrue(dementor.contains("@Inject(method = \"onCreateSource\""));
        assertFalse(dementor.contains("@Inject(method = \"onDamaged\""));
        assertFalse(dementor.contains("AttackCache cache"));
    }

    @Test
    void gravityMixinTargetsTheClassThatDeclaresCanApply() throws IOException {
        String gravity = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/GravityTraitMixin.java"));

        assertTrue(gravity.contains("@Mixin(value = AuraEffectTrait.class"));
        assertTrue(gravity.contains("@Inject(method = \"canApply\""));
        assertTrue(gravity.contains("((Object) this) instanceof GravityTrait"));
        assertFalse(gravity.contains("@Mixin(value = GravityTrait.class"));
    }

    @Test
    void combatSnapshotKeepsBypassFlagsAndRingOrder() {
        var snapshot = new ImmunityHelper.CombatCurioSnapshot(
                true, false, true, List.of(0.65f, 1.25f));

        assertTrue(snapshot.bypassDispell());
        assertFalse(snapshot.bypassDementor());
        assertTrue(snapshot.bypassAdaptive());
        assertEquals(List.of(0.65f, 1.25f), snapshot.ringMultipliers());
    }

    @Test
    void sameRingInstanceOnlyAddsOneMultiplier() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/util/ImmunityHelper.java"));
        String compact = source.replaceAll("\\s+", " ");

        assertTrue(compact.contains("seenRings = Collections.newSetFromMap(new IdentityHashMap<>());"));
        assertTrue(compact.contains("if (!seenRings.add(ring)) return false;"));
        assertTrue(compact.contains("ringMultipliers.add(ring.getDamageMultiplier());"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Boolean> traitCache() throws ReflectiveOperationException {
        return (Map<String, Boolean>) field("traitTagCache").get(null);
    }

    private static AtomicInteger generation() throws ReflectiveOperationException {
        return (AtomicInteger) field("immunityCacheGeneration").get(null);
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = ImmunityHelper.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
