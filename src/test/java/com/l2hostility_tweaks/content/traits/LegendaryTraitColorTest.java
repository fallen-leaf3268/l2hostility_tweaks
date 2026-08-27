package com.l2hostility_tweaks.content.traits;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegendaryTraitColorTest {

    @Test
    void defaultsNullColorToLegendaryGold() {
        assertEquals(ChatFormatting.GOLD.getColor().intValue(),
                LegendaryTraitColor.normalize(null).getAsInt());
    }

    @Test
    void preservesExplicitCustomColor() {
        assertEquals(0x123456, LegendaryTraitColor.normalize(() -> 0x123456).getAsInt());
    }

    @Test
    void formatsLegendaryAttributeDecimalsLikeVanilla() {
        assertEquals("+0.25", LegendaryAttributeTrait.formatAttributeValue(
                0.25, AttributeModifier.Operation.ADDITION, vanillaAttributeFormat()));
        assertEquals("+0.5%", LegendaryAttributeTrait.formatAttributeValue(
                0.005, AttributeModifier.Operation.MULTIPLY_TOTAL, vanillaAttributeFormat()));
        assertEquals("+0.4%", LegendaryAttributeTrait.formatAttributeValue(
                0.004, AttributeModifier.Operation.MULTIPLY_BASE, vanillaAttributeFormat()));
    }

    @Test
    void formatsNegativeLegendaryAttributeValuesWithoutDoubleSign() {
        assertEquals("-0.25", LegendaryAttributeTrait.formatAttributeValue(
                -0.25, AttributeModifier.Operation.ADDITION, vanillaAttributeFormat()));
        assertEquals("-0.5%", LegendaryAttributeTrait.formatAttributeValue(
                -0.005, AttributeModifier.Operation.MULTIPLY_TOTAL, vanillaAttributeFormat()));
    }

    private static DecimalFormat vanillaAttributeFormat() {
        return new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    }
}
