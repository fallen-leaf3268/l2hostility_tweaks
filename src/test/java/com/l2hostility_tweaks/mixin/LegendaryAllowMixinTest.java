package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegendaryAllowMixinTest {

    @Test
    void bypassesUpstreamGateOnlyWhenExplicitlyEnabled() {
        assertEquals(5, LegendaryAllowMixin.l2fix$resolveLegendaryGate(false, 5));
        assertEquals(-1, LegendaryAllowMixin.l2fix$resolveLegendaryGate(true, 5));
    }

    @Test
    void bypassesAntibuildUseGateOnlyWhenEffectAndPermissionAreBothPresent() {
        assertFalse(AntibuildPlaceBypassMixin.l2fix$shouldBypass(false, false));
        assertFalse(AntibuildPlaceBypassMixin.l2fix$shouldBypass(false, true));
        assertFalse(AntibuildPlaceBypassMixin.l2fix$shouldBypass(true, false));
        assertTrue(AntibuildPlaceBypassMixin.l2fix$shouldBypass(true, true));
    }
}
