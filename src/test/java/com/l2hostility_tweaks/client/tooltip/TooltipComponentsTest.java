package com.l2hostility_tweaks.client.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipComponentsTest {

    private static final String NAME = "enchantment.l2hostility_tweaks.reprint_counter";
    private static final String DESC = NAME + ".desc";
    private static final String DESC_ANY = NAME + ".desc_any";

    @Test
    void findsRootAndNestedTranslationKeys() {
        Component root = Component.translatable(DESC);
        Component nested = Component.literal("• ").append(Component.translatable(DESC));

        assertTrue(TooltipComponents.containsTranslation(root, Set.of(DESC)));
        assertTrue(TooltipComponents.containsTranslation(nested, Set.of(DESC)));
        assertFalse(TooltipComponents.containsTranslation(nested, Set.of(DESC_ANY)));
    }

    @Test
    void replacesNestedTranslationAndPreservesWrapper() {
        Component line = Component.literal("• ").append(Component.translatable(DESC));

        Component result = TooltipComponents.replaceTranslations(line, Set.of(DESC),
                Component.translatable(DESC_ANY));

        assertTrue(TooltipComponents.containsTranslation(result, Set.of(DESC_ANY)));
        assertFalse(TooltipComponents.containsTranslation(result, Set.of(DESC)));
        assertEquals("• ", ((LiteralContents) result.getContents()).text());
    }

    @Test
    void replacesRootTranslationAndPreservesUnrelatedSuffix() {
        Component line = Component.translatable(DESC).append(Component.literal(" [source]"));

        Component result = TooltipComponents.replaceTranslations(line, Set.of(DESC),
                Component.translatable(DESC_ANY));

        assertTrue(TooltipComponents.containsTranslation(result, Set.of(DESC_ANY)));
        assertEquals(" [source]", result.getSiblings().get(0).getString());
    }

    @Test
    void replacementPreservesExistingRootStyle() {
        Component line = Component.translatable(DESC).withStyle(ChatFormatting.DARK_AQUA);

        Component result = TooltipComponents.replaceTranslations(line, Set.of(DESC),
                Component.translatable(DESC_ANY).withStyle(ChatFormatting.GRAY));

        assertEquals(line.getStyle(), result.getStyle());
    }

    @Test
    void replacesFirstDescriptionAndRemovesDuplicateLines() {
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Item"),
                Component.translatable(NAME),
                Component.translatable(DESC),
                Component.literal("Other"),
                Component.literal("• ").append(Component.translatable(DESC))));

        TooltipComponents.upsert(tooltip, Set.of(DESC, DESC_ANY), NAME, List.of(NAME),
                Component.translatable(DESC_ANY));

        assertEquals(4, tooltip.size());
        assertEquals(1, tooltip.stream()
                .filter(line -> TooltipComponents.containsTranslation(line, Set.of(DESC_ANY))).count());
        assertEquals("Other", tooltip.get(3).getString());
    }

    @Test
    void insertsAfterMatchingEnchantmentName() {
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Item"),
                Component.translatable(NAME),
                Component.literal("Other")));

        TooltipComponents.upsert(tooltip, Set.of(DESC), NAME, List.of(NAME),
                Component.translatable(DESC));

        assertTrue(TooltipComponents.containsTranslation(tooltip.get(2), Set.of(DESC)));
        assertEquals("Other", tooltip.get(3).getString());
    }

    @Test
    void fallsBackAfterItemNameAndRemainsIdempotent() {
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Item"),
                Component.literal("Other")));

        TooltipComponents.upsert(tooltip, Set.of(DESC), NAME, List.of(),
                Component.translatable(DESC));
        TooltipComponents.upsert(tooltip, Set.of(DESC), NAME, List.of(),
                Component.translatable(DESC));

        assertEquals(3, tooltip.size());
        assertTrue(TooltipComponents.containsTranslation(tooltip.get(1), Set.of(DESC)));
    }

    @Test
    void respectsVanillaHideFlags() {
        int flags = ItemStack.TooltipPart.ENCHANTMENTS.getMask();

        assertTrue(TooltipComponents.isVisible(0, ItemStack.TooltipPart.ENCHANTMENTS));
        assertFalse(TooltipComponents.isVisible(flags, ItemStack.TooltipPart.ENCHANTMENTS));
        assertTrue(TooltipComponents.isVisible(flags, ItemStack.TooltipPart.ADDITIONAL));
    }
}
