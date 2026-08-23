package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegendaryAllowMixinTest {

    @Test
    void bypassesUpstreamGateOnlyWhenExplicitlyEnabled() {
        assertEquals(5, LegendaryAllowMixin.l2fix$resolveLegendaryGate(false, 5));
        assertEquals(-1, LegendaryAllowMixin.l2fix$resolveLegendaryGate(true, 5));
    }
}
