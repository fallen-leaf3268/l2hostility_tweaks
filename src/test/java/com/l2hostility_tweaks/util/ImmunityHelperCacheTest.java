package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void invalidatesTraitAndEntityImmunityCaches() throws ReflectiveOperationException {
        Map<String, Boolean> traitCache = traitCache();
        traitCache.put("l2hostility:test:l2hostility_tweaks:immune_to_force", true);
        setField("cacheTickForce", 20);
        setField("cacheTickGravity", 20);
        setField("cachedEntityForceRef", new WeakReference<>(new Object()));
        setField("cachedEntityGravityRef", new WeakReference<>(new Object()));

        ImmunityHelper.invalidateTagCaches();

        assertTrue(traitCache.isEmpty());
        assertEquals(-1, field("cacheTickForce").getInt(null));
        assertEquals(-1, field("cacheTickGravity").getInt(null));
        assertNull(((WeakReference<?>) field("cachedEntityForceRef").get(null)).get());
        assertNull(((WeakReference<?>) field("cachedEntityGravityRef").get(null)).get());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Boolean> traitCache() throws ReflectiveOperationException {
        return (Map<String, Boolean>) field("traitTagCache").get(null);
    }

    private static void setField(String name, Object value) throws ReflectiveOperationException {
        field(name).set(null, value);
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = ImmunityHelper.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
