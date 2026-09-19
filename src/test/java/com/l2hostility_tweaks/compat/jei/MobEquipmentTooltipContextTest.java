package com.l2hostility_tweaks.compat.jei;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MobEquipmentTooltipContextTest {

    @Test
    void includesEveryGenerationRuleFieldInOrder() {
        CompoundTag stack = stack("example:blade", "charged");
        var rule = new TraitSpawnIndexSnapshot.MobEquipmentRuleView(
                id("example:mob"), "equipment/mainhand", 80, 0.5, 2, 10, true);
        var equipment = new TraitSpawnIndexSnapshot.MobEquipmentView(
                TraitSpawnIndexSnapshot.MobEquipmentCategory.HANDS, stack, List.of(rule));
        var context = new MobEquipmentTooltipContext(page(List.of(equipment)),
                TraitSpawnIndexSnapshot.MobEquipmentCategory.HANDS);

        List<Component> lines = context.tooltipLines(stack);

        assertEquals(List.of(
                "jei.l2hostility_tweaks.equipment_details_header",
                "jei.l2hostility_tweaks.source",
                "jei.l2hostility_tweaks.equipment_slot",
                "jei.l2hostility_tweaks.equipment_min_level",
                "jei.l2hostility_tweaks.equipment_pool_chance",
                "jei.l2hostility_tweaks.equipment_weight",
                "jei.l2hostility_tweaks.equipment_selection_chance",
                "jei.l2hostility_tweaks.equipment_rule_hit_chance",
                "jei.l2hostility_tweaks.equipment_may_be_overwritten"), translationKeys(lines));
        assertEquals("50%", translation(lines.get(4)).getArgs()[0]);
        assertEquals("20%", translation(lines.get(6)).getArgs()[0]);
        assertEquals("10%", translation(lines.get(7)).getArgs()[0]);
    }

    @Test
    void matchesTheCurrentCycledStackIncludingNbt() {
        CompoundTag first = stack("example:blade", "one");
        CompoundTag second = stack("example:blade", "two");
        var equipmentOne = equipment(first, "equipment/mainhand");
        var equipmentTwo = equipment(second, "equipment/offhand");
        var context = new MobEquipmentTooltipContext(page(List.of(equipmentOne, equipmentTwo)),
                TraitSpawnIndexSnapshot.MobEquipmentCategory.HANDS);

        List<Component> lines = context.tooltipLines(first);

        assertEquals(1, translationKeys(lines).stream()
                .filter("jei.l2hostility_tweaks.equipment_slot"::equals).count());
        assertEquals("equipment/mainhand", translation(lines.get(2)).getArgs()[0]);
    }

    private static TraitSpawnIndexSnapshot.MobEquipmentView equipment(CompoundTag stack, String slot) {
        var rule = new TraitSpawnIndexSnapshot.MobEquipmentRuleView(
                id("example:mob"), slot, 0, 1, 1, 1, false);
        return new TraitSpawnIndexSnapshot.MobEquipmentView(
                TraitSpawnIndexSnapshot.MobEquipmentCategory.HANDS, stack, List.of(rule));
    }

    private static TraitSpawnIndexSnapshot.MobTraitOverview page(
            List<TraitSpawnIndexSnapshot.MobEquipmentView> equipment) {
        return new TraitSpawnIndexSnapshot.MobTraitOverview(
                id("minecraft:zombie"), 0, "", List.of(), List.of(), List.of(), List.of(),
                equipment, List.of());
    }

    private static CompoundTag stack(String id, String variant) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", id);
        stack.putByte("Count", (byte) 1);
        CompoundTag tag = new CompoundTag();
        tag.putString("variant", variant);
        stack.put("tag", tag);
        return stack;
    }

    private static List<String> translationKeys(List<Component> components) {
        return components.stream().map(MobEquipmentTooltipContextTest::translation)
                .map(TranslatableContents::getKey).toList();
    }

    private static TranslatableContents translation(Component component) {
        return assertInstanceOf(TranslatableContents.class, component.getContents());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
