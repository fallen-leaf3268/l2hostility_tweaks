package com.l2hostility_tweaks.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipPipelineDescriptionsTest {

    private static final String REPRINT = "enchantment.l2hostility_tweaks.reprint_counter";
    private static final String REPRINT_DESC = REPRINT + ".desc";
    private static final String REPRINT_ARMOR = REPRINT + ".desc_armor";
    private static final String REPRINT_ANY = REPRINT + ".desc_any";
    private static final String GLUTTONY = "enchantment.l2hostility_tweaks.gluttony_pocket";
    private static final String GLUTTONY_DESC = GLUTTONY + ".desc";

    @Test
    void processesTwoDescriptionsWithoutShortCircuiting() {
        Enchantment reprint = new TestEnchantment(REPRINT);
        Enchantment gluttony = new TestEnchantment(GLUTTONY);
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put(reprint, 1);
        enchantments.put(gluttony, 2);
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Pocket"),
                Component.translatable(REPRINT),
                Component.translatable(GLUTTONY)));

        TooltipPipeline.applyDescription(tooltip, enchantments, reprint,
                Set.of(REPRINT_DESC, REPRINT_ARMOR, REPRINT_ANY), Component.translatable(REPRINT_ANY));
        TooltipPipeline.applyDescription(tooltip, enchantments, gluttony,
                Set.of(GLUTTONY_DESC), Component.translatable(GLUTTONY_DESC));

        assertTrue(TooltipComponents.containsTranslation(tooltip, REPRINT_ANY));
        assertTrue(TooltipComponents.containsTranslation(tooltip, GLUTTONY_DESC));
    }

    @Test
    void replacesWrappedDescriptionAndRemovesDuplicate() {
        Enchantment reprint = new TestEnchantment(REPRINT);
        Map<Enchantment, Integer> enchantments = Map.of(reprint, 1);
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Armor"),
                Component.translatable(REPRINT),
                Component.literal("• ").append(Component.translatable(REPRINT_DESC)),
                Component.translatable(REPRINT_DESC)));

        TooltipPipeline.applyDescription(tooltip, enchantments, reprint,
                Set.of(REPRINT_DESC, REPRINT_ARMOR, REPRINT_ANY), Component.translatable(REPRINT_ARMOR));

        assertEquals(3, tooltip.size());
        assertTrue(TooltipComponents.containsTranslation(tooltip, REPRINT_ARMOR));
    }

    @Test
    void buildsCorrectReprintVariant() {
        assertTrue(TooltipComponents.containsTranslation(
                TooltipPipeline.reprintDescription(true, false, 2, 0.1), Set.of(REPRINT_ARMOR)));
        assertTrue(TooltipComponents.containsTranslation(
                TooltipPipeline.reprintDescription(false, true, 10, 0.1), Set.of(REPRINT_DESC)));
        assertTrue(TooltipComponents.containsTranslation(
                TooltipPipeline.reprintDescription(false, false, 2, 0.1), Set.of(REPRINT_ANY)));
    }

    private static final class TestEnchantment extends Enchantment {

        private final String descriptionId;

        private TestEnchantment(String descriptionId) {
            super(Rarity.COMMON, EnchantmentCategory.BREAKABLE, EquipmentSlot.values());
            this.descriptionId = descriptionId;
        }

        @Override
        public String getDescriptionId() {
            return descriptionId;
        }
    }
}
