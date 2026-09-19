package com.l2hostility_tweaks.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiPluginBoundaryTest {

    @Test
    void jeiImportsStayInsideTheOptionalCompatibilityPackage() throws IOException {
        Path javaRoot = Path.of("src/main/java");
        List<Path> violations;
        try (var paths = Files.walk(javaRoot)) {
            violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().replace('\\', '/').contains("/compat/jei/"))
                    .filter(path -> read(path).contains("import mezz.jei"))
                    .toList();
        }

        assertTrue(violations.isEmpty(), () -> "JEI imports outside compat/jei: " + violations);
    }

    @Test
    void pluginRegistersEmptyMobIngredientAndCategory() throws IOException {
        String source = pluginSource();

        assertTrue(source.contains("@JeiPlugin"));
        assertTrue(source.contains("registration.register(MobIngredient.TYPE, List.of(),"));
        assertTrue(source.contains("new MobIngredientHelper(), new MobIngredientRenderer(16)"));
        assertTrue(source.contains("registration.addRecipeCategories(new TraitOverviewCategory(registration.getJeiHelpers()))"));
    }

    @Test
    void pluginForwardsRuntimeLifecycleAndAttachesCacheListenerOnlyOnce() throws IOException {
        String source = pluginSource();

        assertTrue(source.contains("JeiRuntimeBridge.INSTANCE.runtimeAvailable(runtime)"));
        assertTrue(source.contains("JeiRuntimeBridge.INSTANCE.runtimeUnavailable()"));
        assertTrue(source.contains("TraitSpawnClientCache.INSTANCE.addListener(JeiRuntimeBridge.INSTANCE::refresh)"));
        assertTrue(source.contains("compareAndSet(false, true)"));
        assertTrue(source.contains("JeiRuntimeBridge.INSTANCE.refresh(TraitSpawnClientCache.INSTANCE.current())"));
        assertFalse(source.contains("registerRecipes"));
    }

    @Test
    void categorySeparatesUnfilteredTraitDisplaySlotsFromSearchableItemOutputs() throws IOException {
        String source = read(Path.of(
                "src/main/java/com/l2hostility_tweaks/compat/jei/TraitOverviewCategory.java"));
        assertTrue(source.contains("addSlot(RecipeIngredientRole.INPUT, MOB_SLOT_X, MOB_SLOT_Y)"));
        assertTrue(source.contains("addSlot(RecipeIngredientRole.RENDER_ONLY, POOL_SLOT_X, POOL_SLOT_Y)"));
        assertTrue(source.contains("addSlot(RecipeIngredientRole.RENDER_ONLY, BLOCKED_SLOT_X, BLOCKED_SLOT_Y)"));
        assertTrue(source.contains("addSlot(RecipeIngredientRole.RENDER_ONLY, PRESET_SLOT_X, PRESET_SLOT_Y)"));
        assertTrue(source.contains("addItemStacks(resolveStacks(poolItemIds))"));
        assertTrue(source.contains("addItemStacks(resolveStacks(blockedItemIds))"));
        assertTrue(source.contains("addItemStacks(resolveStacks(presetItemIds))"));
        assertTrue(source.contains("addInvisibleIngredients(RecipeIngredientRole.OUTPUT)"));
        assertTrue(source.contains("TraitOverviewPresentation.searchableOutputItemIds(recipe)"));
        assertTrue(source.contains("public void onDisplayedIngredientsUpdate(Object value"));
        assertTrue(source.contains("overrideDisplayedStacks(recipeSlots, \"pool\", poolItemIds)"));
        assertTrue(source.contains("overrideDisplayedStacks(recipeSlots, \"blocked\", blockedItemIds)"));
        assertTrue(source.contains("overrideDisplayedStacks(recipeSlots, \"presets\", presetItemIds)"));
        assertTrue(source.contains("getMethod(\"createDisplayOverrides\")"));
        assertTrue(source.contains("setSlotName(\"pool\")"));
        assertTrue(source.contains("setSlotName(\"blocked\")"));
        assertTrue(source.contains("setSlotName(\"presets\")"));
        assertTrue(source.contains("if (shouldRenderPresets(recipe))"));
        assertFalse(source.contains("guiGraphics.renderItem(blockedStack"));
        assertFalse(source.contains("currentBlocked("));
    }

    @Test
    void newJeiDisplayCallbackKeepsTheGenericInterfaceErasureDescriptor() throws NoSuchMethodException {
        TraitOverviewCategory.class.getDeclaredMethod("onDisplayedIngredientsUpdate",
                Object.class, List.class, IFocusGroup.class);
        TraitOverviewCategory.class.getDeclaredMethod("applyDisplayOverride",
                IRecipeSlotDrawable.class, List.class);
    }

    private static String pluginSource() throws IOException {
        return read(Path.of("src/main/java/com/l2hostility_tweaks/compat/jei/L2HTweaksJeiPlugin.java"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            return "";
        }
    }
}
