package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushCancelMixinTest {

    @Test
    void cancelsPushOnlyForForceImmunity() {
        assertTrue(MixinTestInvoker.<Boolean>call(PushCancelMixin.class, "l2fix$shouldCancelPush", true, false));
        assertTrue(MixinTestInvoker.<Boolean>call(PushCancelMixin.class, "l2fix$shouldCancelPush", true, true));
        assertFalse(MixinTestInvoker.<Boolean>call(PushCancelMixin.class, "l2fix$shouldCancelPush", false, true));
        assertFalse(MixinTestInvoker.<Boolean>call(PushCancelMixin.class, "l2fix$shouldCancelPush", false, false));
    }
}
