package com.l2hostility_tweaks.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class KubeJsRegistryResolverTest {

    @Test
    void legendaryAttributeRejectsNullOperationWithoutThrowing() {
        var builder = new LegendaryAttributeTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "null_operation_test"));

        assertDoesNotThrow(() -> builder.attribute(
                "test", "Invalid Attribute ID", 1.0, null));
    }

    @Test
    void legendaryAttributeRejectsUnknownOperation() {
        var builder = new LegendaryAttributeTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "unknown_operation_test"));

        assertDoesNotThrow(() -> builder.attribute(
                "test", "Invalid Attribute ID", 1.0, "mult_totla"));
    }

    @Test
    void rejectsMalformedIdWithoutCallingRegistry() {
        AtomicBoolean called = new AtomicBoolean();

        Object result = KubeJsRegistryResolver.resolve("mob effect", "Invalid Effect ID", id -> {
            called.set(true);
            return new Object();
        });

        assertNull(result);
        assertFalse(called.get());
    }

    @Test
    void returnsRegisteredValue() {
        Object expected = new Object();

        Object result = KubeJsRegistryResolver.resolve("mob effect", "minecraft:weakness",
                id -> "minecraft:weakness".equals(id.toString()) ? expected : null);

        assertSame(expected, result);
    }

    @Test
    void rejectsUnregisteredValue() {
        assertNull(KubeJsRegistryResolver.resolve("attribute", "othermod:missing", id -> null));
    }

    @Test
    void rejectsNullIdWithoutCallingRegistry() {
        AtomicBoolean called = new AtomicBoolean();

        Object result = KubeJsRegistryResolver.resolve("attribute", null, id -> {
            called.set(true);
            return new Object();
        });

        assertNull(result);
        assertFalse(called.get());
    }

}
