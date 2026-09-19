package com.l2hostility_tweaks.compat.jei;

import com.mojang.datafixers.util.Either;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.client.event.RenderTooltipEvent;

import java.util.Objects;
import java.util.Optional;

public final class TraitIngredientTooltipRegistry {

    private static final String JEI_INGREDIENT_GRID =
            "mezz.jei.common.gui.IngredientGridTooltipComponent";
    private static final ActivationTracker<ActiveContext> ACTIVE_CONTEXT = new ActivationTracker<>();

    public static void activate(TraitSpawnIndexSnapshot.MobTraitOverview overview,
                                TraitIngredientTooltipContext.Section section) {
        ACTIVE_CONTEXT.activate(new ActiveContext(
                Objects.requireNonNull(overview), Objects.requireNonNull(section)));
    }

    public static void appendTooltip(RenderTooltipEvent.GatherComponents event) {
        if (containsJeiIngredientGrid(event)) {
            if (Screen.hasShiftDown()) {
                ACTIVE_CONTEXT.beginNested();
            } else {
                ACTIVE_CONTEXT.clear();
            }
            return;
        }
        if (!Screen.hasShiftDown()) {
            ACTIVE_CONTEXT.clear();
            return;
        }
        ActiveContext active = ACTIVE_CONTEXT.take();
        if (active == null || event.getItemStack().isEmpty()) return;
        var itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        new TraitIngredientTooltipContext(active.section(), active.overview(), itemId).tooltipLines().stream()
                .map(Either::<net.minecraft.network.chat.FormattedText, TooltipComponent>left)
                .forEach(event.getTooltipElements()::add);
    }

    static boolean isJeiIngredientGridClassName(String className) {
        return JEI_INGREDIENT_GRID.equals(className);
    }

    private static boolean containsJeiIngredientGrid(RenderTooltipEvent.GatherComponents event) {
        return event.getTooltipElements().stream()
                .map(element -> element.right())
                .flatMap(Optional::stream)
                .anyMatch(TraitIngredientTooltipRegistry::isJeiIngredientGridComponent);
    }

    private static boolean isJeiIngredientGridComponent(TooltipComponent component) {
        for (Class<?> type = component.getClass(); type != null; type = type.getSuperclass()) {
            if (isJeiIngredientGridClassName(type.getName())) return true;
        }
        return false;
    }

    static final class ActivationTracker<T> {

        private T direct;
        private T nested;

        void activate(T value) {
            direct = Objects.requireNonNull(value);
            nested = null;
        }

        void beginNested() {
            nested = direct;
            direct = null;
        }

        T take() {
            T value = direct != null ? direct : nested;
            clear();
            return value;
        }

        void clear() {
            direct = null;
            nested = null;
        }
    }

    private record ActiveContext(TraitSpawnIndexSnapshot.MobTraitOverview overview,
                                 TraitIngredientTooltipContext.Section section) {
    }

    private TraitIngredientTooltipRegistry() {
    }
}
