package com.l2hostility_tweaks.compat.jei;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TraitIngredientTooltipRegistry {

    private static final String JEI_INGREDIENT_GRID =
            "mezz.jei.common.gui.IngredientGridTooltipComponent";
    private static final WeakIdentityMap<ItemStack, TraitIngredientTooltipContext> CONTEXTS =
            new WeakIdentityMap<>();

    public static void register(ItemStack stack, TraitIngredientTooltipContext context) {
        Objects.requireNonNull(stack);
        Objects.requireNonNull(context);
        CONTEXTS.put(stack, context);
    }

    public static Optional<TraitIngredientTooltipContext> find(ItemStack stack) {
        if (stack == null) return Optional.empty();
        return Optional.ofNullable(CONTEXTS.get(stack));
    }

    public static void appendTooltip(RenderTooltipEvent.GatherComponents event) {
        if (!Screen.hasShiftDown()) return;
        Optional<TraitIngredientTooltipContext> context = find(event.getItemStack());
        if (context.isEmpty() || containsJeiIngredientGrid(event)) return;
        context.get().tooltipLines().stream()
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

    static final class WeakIdentityMap<K, V> {

        private final ReferenceQueue<K> expiredKeys = new ReferenceQueue<>();
        private final Map<IdentityWeakReference<K>, V> values = new HashMap<>();

        void put(K key, V value) {
            removeExpired();
            values.put(new IdentityWeakReference<>(key, expiredKeys), value);
        }

        V get(K key) {
            removeExpired();
            return values.get(new IdentityWeakReference<>(key));
        }

        void clear() {
            values.clear();
            while (expiredKeys.poll() != null) {
            }
        }

        @SuppressWarnings("unchecked")
        private void removeExpired() {
            IdentityWeakReference<K> expired;
            while ((expired = (IdentityWeakReference<K>) expiredKeys.poll()) != null) {
                values.remove(expired);
            }
        }
    }

    private static final class IdentityWeakReference<K> extends WeakReference<K> {

        private final int identityHashCode;

        private IdentityWeakReference(K value) {
            super(value);
            identityHashCode = System.identityHashCode(value);
        }

        private IdentityWeakReference(K value, ReferenceQueue<K> queue) {
            super(value, queue);
            identityHashCode = System.identityHashCode(value);
        }

        @Override
        public int hashCode() {
            return identityHashCode;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof IdentityWeakReference<?> other)) return false;
            K value = get();
            return value != null && value == other.get();
        }
    }

    private TraitIngredientTooltipRegistry() {
    }
}
