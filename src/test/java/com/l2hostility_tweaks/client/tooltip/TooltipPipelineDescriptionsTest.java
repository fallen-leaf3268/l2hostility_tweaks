package com.l2hostility_tweaks.client.tooltip;

import com.google.gson.JsonParser;
import dev.xkmc.l2hostility.content.item.traits.SealedItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipPipelineDescriptionsTest {

    @Test
    void tooltipNumberFormattingDoesNotDependOnSystemLocale() throws IOException {
        Pattern defaultLocaleFormat = Pattern.compile("String\\.format\\((?!Locale\\.ROOT,)");
        for (String file : List.of(
                "src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipPipeline.java",
                "src/main/java/com/l2hostility_tweaks/content/MiracleTwistedPocket.java",
                "src/main/java/com/l2hostility_tweaks/mixin/AdaptingTraitMixin.java",
                "src/main/java/com/l2hostility_tweaks/mixin/KillerAuraTraitMixin.java",
                "src/main/java/com/l2hostility_tweaks/mixin/ReprintTraitMixin.java")) {
            String source = Files.readString(Path.of(file));
            assertFalse(defaultLocaleFormat.matcher(source).find(), file);
        }
    }

    @Test
    void miraclePocketEnglishTooltipUsesIntervalBeforeReduction() throws IOException {
        var lang = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/l2hostility_tweaks/lang/en_us.json"))).getAsJsonObject();
        String tooltip = lang.get("tooltip.l2hostility_tweaks.miracle_twisted_pocket").getAsString();

        assertEquals("Every %s s, the seal time of sealed items is reduced by %s s", tooltip);
    }

    private static final String REPRINT = "enchantment.l2hostility_tweaks.reprint_counter";
    private static final String REPRINT_DESC = REPRINT + ".desc";
    private static final String REPRINT_ARMOR = REPRINT + ".desc_armor";
    private static final String REPRINT_ANY = REPRINT + ".desc_any";
    private static final String GLUTTONY = "enchantment.l2hostility_tweaks.gluttony_pocket";
    private static final String GLUTTONY_DESC = GLUTTONY + ".desc";
    private static final String GLOW_ENABLED = "tooltip.l2hostility_tweaks.glow_enabled";
    private static final String GLOW_DISABLED = "tooltip.l2hostility_tweaks.glow_disabled";
    private static final String SPLIT_UPSTREAM_DESC =
            "enchantment.l2hostility.split_suppressor.desc";
    private static final String SPLIT_OWNED_DESC =
            "enchantment.l2hostility_tweaks.split_suppressor.desc";

    @Test
    void splitSuppressorUsesOwnedTranslationKey() {
        Component description = TooltipPipeline.splitSuppressorDescription();

        assertTrue(TooltipComponents.containsTranslation(description, Set.of(SPLIT_OWNED_DESC)));
        assertEquals(Set.of(SPLIT_UPSTREAM_DESC, SPLIT_OWNED_DESC),
                TooltipPipeline.splitSuppressorDescriptionKeys());
    }

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

    @Test
    void armorReprintReductionTooltipUsesRuntimeEightyPercentCap() {
        Component tooltip = TooltipPipeline.reprintDescription(true, false, 5, 0.3);
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, tooltip.getContents());
        Component reduction = assertInstanceOf(Component.class, contents.getArgs()[0]);

        assertEquals("80%", reduction.getString());
    }

    @Test
    void staticItemTooltipReplacesCanonicalLineOnly() {
        Component repeatedName = Component.literal("Sealed item");
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Detector Glasses"),
                Component.translatable(GLOW_ENABLED),
                repeatedName,
                repeatedName));

        TooltipPipeline.applyStaticItemTooltip(tooltip, Set.of(GLOW_ENABLED, GLOW_DISABLED),
                Component.translatable(GLOW_DISABLED));

        assertEquals(4, tooltip.size());
        assertTrue(TooltipComponents.containsTranslation(tooltip, GLOW_DISABLED));
        assertEquals(2, tooltip.stream().filter(repeatedName::equals).count());
    }

    @Test
    void restorationPocketReportsContentsOnlyWhenAValidStoredItemExists() throws IOException {
        CompoundTag emptyPocket = new CompoundTag();
        emptyPocket.putString("Enchantments", "present");

        assertFalse(TooltipPipeline.hasStoredPocketContents(emptyPocket));
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/client/tooltip/TooltipPipeline.java"));
        String compact = source.replaceAll("\\s+", " ");
        assertTrue(compact.contains("slot.contains(SealedItem.DATA, Tag.TAG_COMPOUND) " +
                "&& !ItemStack.of(slot.getCompound(SealedItem.DATA)).isEmpty()"));
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
