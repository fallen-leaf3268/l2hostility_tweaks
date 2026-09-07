package com.l2hostility_tweaks.compat.jei;

import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record MobIngredient(ResourceLocation entityId) {

    public static final IIngredientType<MobIngredient> TYPE = () -> MobIngredient.class;

    public MobIngredient {
        Objects.requireNonNull(entityId, "entityId");
    }
}
