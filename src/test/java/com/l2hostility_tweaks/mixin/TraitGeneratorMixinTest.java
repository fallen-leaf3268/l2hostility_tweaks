package com.l2hostility_tweaks.mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitGeneratorMixinTest {

    @Test
    void appliesNbtPresetOnlyWhenItRaisesTheCurrentRank() {
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(0, 2));
        assertTrue(TraitGeneratorMixin.l2fix$shouldApplyPreset(1, 3));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(3, 1));
        assertFalse(TraitGeneratorMixin.l2fix$shouldApplyPreset(2, 2));
    }

    @Test
    void parsesValidNbtPresetTraitId() {
        assertEquals("l2hostility:tank",
                TraitGeneratorMixin.l2fix$parseTraitId("l2hostility:tank").toString());
    }

    @Test
    void rejectsMalformedNbtPresetTraitIdWithoutThrowing() {
        assertNull(TraitGeneratorMixin.l2fix$parseTraitId("Invalid Trait ID"));
    }

    @Test
    void rejectsNullNbtPresetTraitId() {
        assertNull(TraitGeneratorMixin.l2fix$parseTraitId(null));
    }

    @Test
    void preparesNbtAndFiltersBeforeTheOriginalInitializationLoop() throws Exception {
        List<String> phases = new ArrayList<>();
        Method pipeline = Arrays.stream(TraitGeneratorMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("l2fix$runBeforeInitializationPipeline"))
                .findFirst()
                .orElse(null);

        assertNotNull(pipeline);
        pipeline.setAccessible(true);
        pipeline.invoke(null, (Runnable) () -> phases.add("nbt"), (Runnable) () -> phases.add("filter"));

        assertEquals(List.of("nbt", "filter"), phases);

        Method preparation = Arrays.stream(TraitGeneratorMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("l2fix$prepareFinalTraits"))
                .findFirst()
                .orElse(null);
        assertNotNull(preparation);
        Inject inject = preparation.getAnnotation(Inject.class);
        assertNotNull(inject);
        At at = inject.at()[0];
        assertEquals("INVOKE", at.value());
        assertEquals("Ljava/util/HashMap;entrySet()Ljava/util/Set;", at.target());
        assertEquals(At.Shift.BEFORE, at.shift());
    }
}
