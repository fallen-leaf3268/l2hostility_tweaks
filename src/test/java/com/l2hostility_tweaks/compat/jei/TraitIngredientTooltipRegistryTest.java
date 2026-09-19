package com.l2hostility_tweaks.compat.jei;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitIngredientTooltipRegistryTest {

    @Test
    void contextsMatchOnlyTheExactRegisteredStackInstance() {
        var map = new TraitIngredientTooltipRegistry.WeakIdentityMap<Object, String>();
        Object registered = new String("same value");
        Object equalButDistinct = new String("same value");

        map.put(registered, "context");

        assertEquals("context", map.get(registered));
        assertEquals(null, map.get(equalButDistinct));
    }

    @Test
    void recognizesOnlyJeiIngredientGridClassNames() {
        assertTrue(TraitIngredientTooltipRegistry.isJeiIngredientGridClassName(
                "mezz.jei.common.gui.IngredientGridTooltipComponent"));
        assertFalse(TraitIngredientTooltipRegistry.isJeiIngredientGridClassName(
                "mezz.jei.gui.recipes.PinnedTooltipRenderer"));
    }

}
