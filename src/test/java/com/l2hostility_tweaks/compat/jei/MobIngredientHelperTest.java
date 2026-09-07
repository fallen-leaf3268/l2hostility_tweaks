package com.l2hostility_tweaks.compat.jei;

import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.ingredients.IIngredientHelper;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobIngredientHelperTest {

    @Test
    void usesEntityIdForStableIdentityAndSearchMetadata() throws NoSuchMethodException {
        MobIngredient ingredient = new MobIngredient(new ResourceLocation("minecraft:zombie"));
        MobIngredientHelper helper = new MobIngredientHelper(id -> "僵尸");

        assertEquals("minecraft:zombie", helper.getUniqueId(ingredient, UidContext.Ingredient));
        assertEquals("僵尸", helper.getDisplayName(ingredient));
        assertEquals(new ResourceLocation("minecraft:zombie"), helper.getResourceLocation(ingredient));
        assertTrue(IIngredientHelper.class.getMethod("getCheatItemStack", Object.class).isDefault());
    }

    @Test
    void exposesTheConcreteIngredientClassAndCopiesImmutableValues() {
        MobIngredient ingredient = new MobIngredient(new ResourceLocation("minecraft:skeleton"));
        MobIngredientHelper helper = new MobIngredientHelper(ResourceLocation::toString);

        assertSame(MobIngredient.class, MobIngredient.TYPE.getIngredientClass());
        assertSame(MobIngredient.TYPE, helper.getIngredientType());
        assertSame(ingredient, helper.copyIngredient(ingredient));
        assertThrows(NullPointerException.class, () -> new MobIngredient(null));
    }

    @Test
    void fallsBackToEntityIdWhenTheNameCannotBeResolved() {
        MobIngredient ingredient = new MobIngredient(new ResourceLocation("example:unknown"));
        MobIngredientHelper helper = new MobIngredientHelper(id -> null);

        assertEquals("example:unknown", helper.getDisplayName(ingredient));
        assertEquals("example:unknown", helper.getErrorInfo(ingredient));
    }
}
