package com.l2hostility_tweaks.compat.jei;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitIngredientTooltipRegistryTest {

    @Test
    void activationSupportsDirectAndNestedIngredientTooltips() {
        var tracker = new TraitIngredientTooltipRegistry.ActivationTracker<Object>();
        Object direct = new Object();
        Object nested = new Object();

        tracker.activate(direct);
        assertSame(direct, tracker.take());
        assertNull(tracker.take());

        tracker.activate(nested);
        tracker.beginNested();
        assertSame(nested, tracker.take());
        assertNull(tracker.take());
    }

    @Test
    void newerActivationReplacesStaleNestedContext() {
        var tracker = new TraitIngredientTooltipRegistry.ActivationTracker<Object>();
        Object stale = new Object();
        Object current = new Object();

        tracker.activate(stale);
        tracker.beginNested();
        tracker.activate(current);

        assertSame(current, tracker.take());
        assertNull(tracker.take());
    }

    @Test
    void recognizesOnlyJeiIngredientGridClassNames() {
        assertTrue(TraitIngredientTooltipRegistry.isJeiIngredientGridClassName(
                "mezz.jei.common.gui.IngredientGridTooltipComponent"));
        assertFalse(TraitIngredientTooltipRegistry.isJeiIngredientGridClassName(
                "mezz.jei.gui.recipes.PinnedTooltipRenderer"));
    }

}
