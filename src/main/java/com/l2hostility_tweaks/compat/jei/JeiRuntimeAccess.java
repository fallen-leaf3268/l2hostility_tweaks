package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;

import java.util.Collection;
import java.util.List;

interface JeiRuntimeAccess {

    void addMobIngredients(Collection<MobIngredient> ingredients);

    void removeMobIngredients(Collection<MobIngredient> ingredients);

    void hidePages(Collection<TraitSpawnIndexSnapshot.MobTraitOverview> pages);

    void addPages(List<TraitSpawnIndexSnapshot.MobTraitOverview> pages);
}
