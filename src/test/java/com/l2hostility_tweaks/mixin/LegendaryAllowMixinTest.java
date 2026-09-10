package com.l2hostility_tweaks.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegendaryAllowMixinTest {

    @Test
    void bypassesUpstreamGateOnlyWhenExplicitlyEnabled() {
        assertEquals(5, MixinTestInvoker.<Integer>call(
                LegendaryAllowMixin.class, "l2fix$resolveLegendaryGate", false, 5));
        assertEquals(-1, MixinTestInvoker.<Integer>call(
                LegendaryAllowMixin.class, "l2fix$resolveLegendaryGate", true, 5));
    }

    @Test
    void configuredLegendaryTraitsUseTheNativeLevelGate() {
        assertFalse(MixinTestInvoker.<Boolean>call(
                MobTraitLegendaryMixin.class, "l2fix$passesGate", true, false, 5, 5));
        assertTrue(MixinTestInvoker.<Boolean>call(
                MobTraitLegendaryMixin.class, "l2fix$passesGate", true, false, 6, 5));
        assertTrue(MixinTestInvoker.<Boolean>call(
                MobTraitLegendaryMixin.class, "l2fix$passesGate", true, true, 0, 5));
        assertTrue(MixinTestInvoker.<Boolean>call(
                MobTraitLegendaryMixin.class, "l2fix$passesGate", false, false, 0, 5));
    }

    @Test
    void configuredLegendaryTraitsUseTheNativeGlobalToggle() {
        assertTrue(MixinTestInvoker.<Boolean>call(
                MobTraitLegendaryMixin.class, "l2fix$isBannedByLegendaryToggle", true, false));
        assertFalse(MixinTestInvoker.<Boolean>call(
                MobTraitLegendaryMixin.class, "l2fix$isBannedByLegendaryToggle", true, true));
        assertFalse(MixinTestInvoker.<Boolean>call(
                MobTraitLegendaryMixin.class, "l2fix$isBannedByLegendaryToggle", false, false));
    }

    @Test
    void registersTheMobTraitSemanticMixin() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/mixin/MobTraitLegendaryMixin.java"));
        var config = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/l2hostility_tweaks.mixins.json"))).getAsJsonObject();

        assertTrue(source.contains("@Mixin(value = MobTrait.class, remap = false)"));
        assertTrue(source.contains("allow(Lnet/minecraft/world/entity/LivingEntity;II)Z"));
        assertTrue(source.contains("isBanned()Z"));
        assertTrue(config.getAsJsonArray("mixins").asList().stream()
                .anyMatch(element -> element.getAsString().equals("MobTraitLegendaryMixin")));
    }
}
