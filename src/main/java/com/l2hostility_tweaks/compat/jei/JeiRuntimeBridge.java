package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JeiRuntimeBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("l2htweaks:jei");
    private static final TraitSpawnIndexSnapshot EMPTY =
            new TraitSpawnIndexSnapshot(Long.MIN_VALUE, List.of(), 0);

    public static final JeiRuntimeBridge INSTANCE = new JeiRuntimeBridge();

    static final RecipeType<TraitSpawnIndexSnapshot.MobTraitOverview> PAGE_TYPE = RecipeType.create(
            "l2hostility_tweaks", "mob_traits", TraitSpawnIndexSnapshot.MobTraitOverview.class);

    private TraitSpawnIndexSnapshot latest = EMPTY;
    private JeiRuntimeAccess access;
    private Map<ResourceLocation, MobIngredient> appliedIngredients = Map.of();
    private List<TraitSpawnIndexSnapshot.MobTraitOverview> appliedPages = List.of();

    public synchronized void runtimeAvailable(IJeiRuntime runtime) {
        Objects.requireNonNull(runtime, "runtime");
        runtimeAvailable(new RuntimeAccess(runtime));
    }

    synchronized void runtimeAvailable(JeiRuntimeAccess access) {
        this.access = Objects.requireNonNull(access, "access");
        appliedIngredients = Map.of();
        appliedPages = List.of();
        applyLatest();
    }

    public synchronized void runtimeUnavailable() {
        access = null;
        appliedIngredients = Map.of();
        appliedPages = List.of();
    }

    public synchronized void refresh(TraitSpawnIndexSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.revision() == latest.revision()) return;
        latest = snapshot;
        applyLatest();
    }

    private void applyLatest() {
        if (access == null) return;

        Map<ResourceLocation, MobIngredient> currentIngredients = new LinkedHashMap<>();
        for (TraitSpawnIndexSnapshot.MobTraitOverview page : latest.mobs()) {
            currentIngredients.putIfAbsent(page.entityId(), new MobIngredient(page.entityId()));
        }

        List<MobIngredient> removed = appliedIngredients.entrySet().stream()
                .filter(entry -> !currentIngredients.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!removed.isEmpty()) access.removeMobIngredients(removed);

        List<MobIngredient> added = currentIngredients.entrySet().stream()
                .filter(entry -> !appliedIngredients.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!added.isEmpty()) access.addMobIngredients(added);

        if (!appliedPages.isEmpty()) access.hidePages(appliedPages);
        List<TraitSpawnIndexSnapshot.MobTraitOverview> currentPages = List.copyOf(latest.mobs());
        if (!currentPages.isEmpty()) access.addPages(currentPages);

        LOGGER.info("JEI_TRAIT_INDEX jei-applied revision={} mobIngredients={} pages={} addedIngredients={} removedIngredients={}",
                latest.revision(), currentIngredients.size(), currentPages.size(), added.size(), removed.size());

        appliedIngredients = Collections.unmodifiableMap(new LinkedHashMap<>(currentIngredients));
        appliedPages = currentPages;
    }

    private record RuntimeAccess(IJeiRuntime runtime) implements JeiRuntimeAccess {

        @Override
        public void addMobIngredients(Collection<MobIngredient> ingredients) {
            runtime.getIngredientManager().addIngredientsAtRuntime(MobIngredient.TYPE, ingredients);
        }

        @Override
        public void removeMobIngredients(Collection<MobIngredient> ingredients) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(MobIngredient.TYPE, ingredients);
        }

        @Override
        public void hidePages(Collection<TraitSpawnIndexSnapshot.MobTraitOverview> pages) {
            runtime.getRecipeManager().hideRecipes(PAGE_TYPE, pages);
        }

        @Override
        public void addPages(List<TraitSpawnIndexSnapshot.MobTraitOverview> pages) {
            runtime.getRecipeManager().addRecipes(PAGE_TYPE, pages);
        }
    }
}
