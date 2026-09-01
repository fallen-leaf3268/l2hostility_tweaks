package com.l2hostility_tweaks.compat.kubejs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubeJsRegistryResolverTest {

    @Test
    void legendaryAttributeRequiresNonBlankModifierName() {
        assertFalse(LegendaryAttributeTraitBuilder.l2fix$isValidName(null));
        assertFalse(LegendaryAttributeTraitBuilder.l2fix$isValidName(""));
        assertFalse(LegendaryAttributeTraitBuilder.l2fix$isValidName("   "));
        assertTrue(LegendaryAttributeTraitBuilder.l2fix$isValidName("legendary_health"));
    }

    @Test
    void legendaryAttributeAcceptsOnlyFiniteNonZeroFactors() {
        assertTrue(LegendaryAttributeTraitBuilder.l2fix$isValidFactor(0.25));
        assertTrue(LegendaryAttributeTraitBuilder.l2fix$isValidFactor(-0.25));
        assertFalse(LegendaryAttributeTraitBuilder.l2fix$isValidFactor(0));
        assertFalse(LegendaryAttributeTraitBuilder.l2fix$isValidFactor(Double.NaN));
        assertFalse(LegendaryAttributeTraitBuilder.l2fix$isValidFactor(Double.POSITIVE_INFINITY));
        assertFalse(LegendaryAttributeTraitBuilder.l2fix$isValidFactor(Double.NEGATIVE_INFINITY));
    }

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
    void explicitInvalidLegendaryAttributePreventsPartialRegistration() {
        var builder = new LegendaryAttributeTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_legendary_attribute"));

        builder.attribute("", "minecraft:generic.max_health", 0.5, "mult_total");

        var error = assertThrows(IllegalStateException.class, builder::createObject);
        assertTrue(error.getMessage().contains("l2hostility_tweaks:invalid_legendary_attribute"));
        assertTrue(error.getMessage().contains("modifier name"));
    }

    @Test
    void emptyLegendaryAttributePreventsInertRegistration() {
        var builder = new LegendaryAttributeTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "empty_legendary_attribute"));

        var error = assertThrows(IllegalStateException.class, builder::createObject);
        assertTrue(error.getMessage().contains("l2hostility_tweaks:empty_legendary_attribute"));
        assertTrue(error.getMessage().contains("at least one attribute"));
    }

    @Test
    void legendaryAttributeModifierNameIsNamespacedByTraitId() {
        var first = new ResourceLocation("example", "fire_titan");
        var second = new ResourceLocation("example", "frost_titan");

        String firstName = LegendaryAttributeTraitBuilder.l2fix$modifierName(first, "health_bonus");
        String secondName = LegendaryAttributeTraitBuilder.l2fix$modifierName(second, "health_bonus");

        assertNotEquals(firstName, secondName);
        assertTrue(firstName.contains("example:fire_titan"));
        assertTrue(firstName.endsWith("health_bonus"));
    }

    @Test
    void legendaryAttributeDuplicateKeyIncludesAttributeAndDeclaredName() {
        var health = new ResourceLocation("minecraft", "generic.max_health");
        var armor = new ResourceLocation("minecraft", "generic.armor");

        assertEquals(LegendaryAttributeTraitBuilder.l2fix$entryKey(health, "bonus"),
                LegendaryAttributeTraitBuilder.l2fix$entryKey(health, "bonus"));
        assertNotEquals(LegendaryAttributeTraitBuilder.l2fix$entryKey(health, "bonus"),
                LegendaryAttributeTraitBuilder.l2fix$entryKey(armor, "bonus"));
        assertNotEquals(LegendaryAttributeTraitBuilder.l2fix$entryKey(health, "bonus"),
                LegendaryAttributeTraitBuilder.l2fix$entryKey(health, "other"));
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

    @Test
    void legendaryTargetEffectKeepsPreviousFunctionAfterInvalidEffectId() throws Exception {
        var builder = new LegendaryTargetEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_effect_preserves_previous"));
        Function<Integer, MobEffectInstance> previous = level -> null;
        Field func = LegendaryTargetEffectTraitBuilder.class.getDeclaredField("func");
        func.setAccessible(true);
        func.set(builder, previous);

        builder.fixedDuration("Invalid Effect ID", 200);

        assertSame(previous, func.get(builder));
    }

    @Test
    void explicitInvalidRegularSelfEffectPreventsFallbackRegistration() {
        var builder = new SelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_regular_self_effect"));

        builder.effect("Invalid Effect ID");

        var error = assertThrows(IllegalStateException.class, builder::createObject);
        assertTrue(error.getMessage().contains("l2hostility_tweaks:invalid_regular_self_effect"));
        assertTrue(error.getMessage().contains("Invalid Effect ID"));
    }

    @Test
    void explicitInvalidLegendarySelfEffectPreventsFallbackRegistration() {
        var builder = new LegendarySelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_legendary_self_effect"));

        builder.effect("Invalid Effect ID");

        var error = assertThrows(IllegalStateException.class, builder::createObject);
        assertTrue(error.getMessage().contains("l2hostility_tweaks:invalid_legendary_self_effect"));
        assertTrue(error.getMessage().contains("Invalid Effect ID"));
    }

    @Test
    void explicitInvalidLegendaryTargetEffectPreventsFallbackRegistration() {
        var builder = new LegendaryTargetEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_legendary_target_effect"));

        builder.fixedDuration("Invalid Effect ID", 200);

        var error = assertThrows(IllegalStateException.class, builder::createObject);
        assertTrue(error.getMessage().contains("l2hostility_tweaks:invalid_legendary_target_effect"));
        assertTrue(error.getMessage().contains("Invalid Effect ID"));
    }

    @Test
    void explicitInvalidLegendaryTargetParametersPreventFallbackRegistration() {
        var builder = new LegendaryTargetEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_legendary_target_duration"));

        builder.fixedDuration("minecraft:weakness", 0);

        var error = assertThrows(IllegalStateException.class, builder::createObject);
        assertTrue(error.getMessage().contains("l2hostility_tweaks:invalid_legendary_target_duration"));
        assertTrue(error.getMessage().contains("duration=0"));
    }

    @Test
    void legendaryTargetEffectRequiresPositiveDuration() {
        assertFalse(LegendaryTargetEffectTraitBuilder.l2fix$isValidDuration(-1));
        assertFalse(LegendaryTargetEffectTraitBuilder.l2fix$isValidDuration(0));
        assertTrue(LegendaryTargetEffectTraitBuilder.l2fix$isValidDuration(1));
    }

    @Test
    void legendaryTargetEffectDurationMultiplicationSaturatesOnOverflow() {
        assertEquals(2400, LegendaryTargetEffectTraitBuilder.l2fix$saturatingDuration(120, 20));
        assertEquals(Integer.MAX_VALUE,
                LegendaryTargetEffectTraitBuilder.l2fix$saturatingDuration(120_000_000, 20));
    }

    @Test
    void legendaryTargetEffectRequiresNonNegativeAmplifier() {
        assertFalse(LegendaryTargetEffectTraitBuilder.l2fix$isValidAmplifier(-1));
        assertTrue(LegendaryTargetEffectTraitBuilder.l2fix$isValidAmplifier(0));
    }

    @Test
    void selfEffectBuildersIgnoreNegativeAmplifierAndAcceptZero() throws Exception {
        var regular = new SelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "regular_amplifier_validation"));
        var legendary = new LegendarySelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "legendary_amplifier_validation"));

        regular.effectLevel(2).effectLevel(-1);
        legendary.effectLevel(2).effectLevel(-1);
        assertEquals(2, amplifierPerLevel(regular));
        assertEquals(2, amplifierPerLevel(legendary));

        regular.effectLevel(0);
        legendary.effectLevel(0);
        assertEquals(0, amplifierPerLevel(regular));
        assertEquals(0, amplifierPerLevel(legendary));
    }

    @Test
    void selfEffectBuildersRejectExplicitNonPositiveDuration() {
        var regular = new SelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_regular_duration"));
        var legendary = new LegendarySelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_legendary_duration"));

        regular.time(0);
        legendary.time(-1);

        assertTrue(assertThrows(IllegalStateException.class, regular::createObject)
                .getMessage().contains("duration=0"));
        assertTrue(assertThrows(IllegalStateException.class, legendary::createObject)
                .getMessage().contains("duration=-1"));
    }

    @Test
    void selfEffectBuildersRejectNegativeAmplifier() {
        var regular = new SelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_regular_amplifier"));
        var legendary = new LegendarySelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "invalid_legendary_amplifier"));

        regular.effectLevel(-1);
        legendary.effectLevel(-1);

        assertTrue(assertThrows(IllegalStateException.class, regular::createObject)
                .getMessage().contains("amplifierPerLevel=-1"));
        assertTrue(assertThrows(IllegalStateException.class, legendary::createObject)
                .getMessage().contains("amplifierPerLevel=-1"));
    }

    @Test
    void validSelfEffectFieldDoesNotClearAnotherFieldsError() {
        var regular = new SelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "independent_regular_errors"));
        var legendary = new LegendarySelfEffectTraitBuilder(
                new ResourceLocation("l2hostility_tweaks", "independent_legendary_errors"));

        regular.time(0).effectLevel(0);
        legendary.time(0).effectLevel(0);

        assertTrue(assertThrows(IllegalStateException.class, regular::createObject)
                .getMessage().contains("duration=0"));
        assertTrue(assertThrows(IllegalStateException.class, legendary::createObject)
                .getMessage().contains("duration=0"));
    }

    private static int amplifierPerLevel(Object builder) throws Exception {
        Field field = builder.getClass().getDeclaredField("amplifierPerLevel");
        field.setAccessible(true);
        return field.getInt(builder);
    }

}
