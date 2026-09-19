package com.l2hostility_tweaks.generation;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobEquipmentCategory;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot.MobEquipmentRuleView;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobEquipmentIndexTest {

    @Test
    void classifiesOnlySupportedDatapackSlots() {
        for (String slot : List.of("equipment/head", "equipment/chest", "equipment/legs", "equipment/feet")) {
            assertEquals(MobEquipmentCategory.ARMOR, MobEquipmentIndex.category(slot));
        }
        for (String slot : List.of("equipment/mainhand", "equipment/offhand")) {
            assertEquals(MobEquipmentCategory.HANDS, MobEquipmentIndex.category(slot));
        }
        assertEquals(MobEquipmentCategory.CURIOS, MobEquipmentIndex.category("curios/ring"));
        assertNull(MobEquipmentIndex.category("equipment/body"));
        assertNull(MobEquipmentIndex.category("curios/"));
        assertNull(MobEquipmentIndex.category(null));
    }

    @Test
    void calculatesSelectionAndRuleHitProbability() {
        MobEquipmentRuleView rule = new MobEquipmentRuleView(
                id("example:mob"), "curios/ring", 60, 0.25, 2, 8, false);

        assertEquals(0.25, rule.poolSelectionChance(), 1.0e-9);
        assertEquals(0.0625, rule.ruleHitChance(), 1.0e-9);
    }

    @Test
    void detectsWhetherTheSameSlotHasALaterPool() {
        List<String> slots = List.of("equipment/mainhand", "curios/ring", "equipment/mainhand");

        assertTrue(MobEquipmentIndex.hasLaterSlot(slots, 0));
        assertFalse(MobEquipmentIndex.hasLaterSlot(slots, 1));
        assertFalse(MobEquipmentIndex.hasLaterSlot(slots, 2));
    }

    @Test
    void captureKeepsFullStacksAndMergesOnlyEqualNbtVariants() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/generation/MobEquipmentIndex.java"));

        assertTrue(source.contains("entry.stack()"));
        assertTrue(source.contains("ItemStack.isSameItemSameTags(existing.stack(), stack)"));
        assertTrue(source.contains("new MobEquipmentView(category, stack, rules)"));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
