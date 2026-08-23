package com.l2hostility_tweaks.client.tooltip;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

public final class TooltipComponents {

    private TooltipComponents() {
    }

    public static boolean containsTranslation(Component component, Collection<String> keys) {
        if (component.getContents() instanceof TranslatableContents translatable
                && keys.contains(translatable.getKey())) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (containsTranslation(sibling, keys)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsTranslation(List<Component> components, String key) {
        return components.stream().anyMatch(component -> containsTranslation(component, List.of(key)));
    }

    public static Component replaceTranslations(Component component, Collection<String> keys,
                                                Component replacement) {
        if (component.getContents() instanceof TranslatableContents translatable
                && keys.contains(translatable.getKey())) {
            return replacement.copy();
        }
        MutableComponent copy = component.copy();
        copy.getSiblings().clear();
        for (Component sibling : component.getSiblings()) {
            copy.append(replaceTranslations(sibling, keys, replacement));
        }
        return copy;
    }

    public static void upsert(List<Component> tooltip, Collection<String> descriptionKeys,
                              String enchantmentNameKey, Collection<String> allEnchantmentNameKeys,
                              Component replacement) {
        int first = -1;
        for (int i = 0; i < tooltip.size(); ) {
            Component line = tooltip.get(i);
            if (!containsTranslation(line, descriptionKeys)) {
                i++;
                continue;
            }
            if (first < 0) {
                tooltip.set(i, replaceTranslations(line, descriptionKeys, replacement));
                first = i;
                i++;
            } else {
                tooltip.remove(i);
            }
        }
        if (first >= 0) {
            return;
        }

        int insertAt = findLastTranslation(tooltip, List.of(enchantmentNameKey));
        if (insertAt < 0) {
            insertAt = findLastTranslation(tooltip, allEnchantmentNameKeys);
        }
        insertAt = insertAt >= 0 ? insertAt + 1 : Math.min(1, tooltip.size());
        tooltip.add(insertAt, replacement);
    }

    public static void upsertOrAppend(List<Component> tooltip, Collection<String> keys,
                                      Component replacement) {
        int first = -1;
        for (int i = 0; i < tooltip.size(); ) {
            Component line = tooltip.get(i);
            if (!containsTranslation(line, keys)) {
                i++;
                continue;
            }
            if (first < 0) {
                tooltip.set(i, replaceTranslations(line, keys, replacement));
                first = i;
                i++;
            } else {
                tooltip.remove(i);
            }
        }
        if (first < 0) {
            tooltip.add(replacement);
        }
    }

    public static boolean isVisible(ItemStack stack, ItemStack.TooltipPart part) {
        int flags = stack.hasTag() && stack.getTag().contains("HideFlags", Tag.TAG_ANY_NUMERIC)
                ? stack.getTag().getInt("HideFlags")
                : stack.getItem().getDefaultTooltipHideFlags(stack);
        return isVisible(flags, part);
    }

    static boolean isVisible(int flags, ItemStack.TooltipPart part) {
        return (flags & part.getMask()) == 0;
    }

    private static int findLastTranslation(List<Component> tooltip, Collection<String> keys) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            if (containsTranslation(tooltip.get(i), keys)) {
                return i;
            }
        }
        return -1;
    }
}
