package com.l2hostility_tweaks.compat.jei;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
import java.util.function.Function;

public final class MobIngredientHelper implements IIngredientHelper<MobIngredient> {

    private final Function<ResourceLocation, String> nameResolver;

    public MobIngredientHelper() {
        this(MobIngredientHelper::resolveName);
    }

    MobIngredientHelper(Function<ResourceLocation, String> nameResolver) {
        this.nameResolver = Objects.requireNonNull(nameResolver, "nameResolver");
    }

    @Override
    public IIngredientType<MobIngredient> getIngredientType() {
        return MobIngredient.TYPE;
    }

    @Override
    public String getDisplayName(MobIngredient ingredient) {
        String name = nameResolver.apply(ingredient.entityId());
        return name == null || name.isBlank() ? ingredient.entityId().toString() : name;
    }

    @Override
    public String getUniqueId(MobIngredient ingredient, UidContext context) {
        return ingredient.entityId().toString();
    }

    @Override
    public ResourceLocation getResourceLocation(MobIngredient ingredient) {
        return ingredient.entityId();
    }

    @Override
    public MobIngredient copyIngredient(MobIngredient ingredient) {
        return ingredient;
    }

    @Override
    public String getErrorInfo(MobIngredient ingredient) {
        return ingredient == null ? "null" : ingredient.entityId().toString();
    }

    private static String resolveName(ResourceLocation entityId) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                .map(type -> type.getDescription().getString())
                .filter(name -> !name.isBlank())
                .orElse(entityId.toString());
    }
}
