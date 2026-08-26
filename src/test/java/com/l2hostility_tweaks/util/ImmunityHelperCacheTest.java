package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
    void generationIsVisibleAcrossQueryThreads() throws ReflectiveOperationException {
        assertTrue(Modifier.isVolatile(field("immunityCacheGeneration").getModifiers()));
    }

    @Test
    void invalidationClearsTraitCacheAndAdvancesEntityGeneration() throws ReflectiveOperationException {
        Map<String, Boolean> traitCache = traitCache();
        traitCache.put("l2hostility:test:l2hostility_tweaks:immune_to_force", true);
        int beforeGeneration = field("immunityCacheGeneration").getInt(null);
        long beforeStamp = ImmunityHelper.cacheStamp(beforeGeneration, 20);

        ImmunityHelper.invalidateTagCaches();

        assertTrue(traitCache.isEmpty());
        int afterGeneration = field("immunityCacheGeneration").getInt(null);
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

    @SuppressWarnings("unchecked")
    private static Map<String, Boolean> traitCache() throws ReflectiveOperationException {
        return (Map<String, Boolean>) field("traitTagCache").get(null);
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = ImmunityHelper.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
