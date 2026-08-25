package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushCancelMixinTest {

    @Test
    void cancelsPushOnlyForForceImmunity() {
        assertTrue(PushCancelMixin.l2fix$shouldCancelPush(true, false));
        assertTrue(PushCancelMixin.l2fix$shouldCancelPush(true, true));
        assertFalse(PushCancelMixin.l2fix$shouldCancelPush(false, true));
        assertFalse(PushCancelMixin.l2fix$shouldCancelPush(false, false));
    }
}
