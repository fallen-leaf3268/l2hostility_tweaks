package com.l2hostility_tweaks.content.traits;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void detectsAttributeAmountOverflowAtAppliedLevel() {
        assertTrue(LegendaryAttributeTrait.hasFiniteAmount(1.5, 200));
        assertFalse(LegendaryAttributeTrait.hasFiniteAmount(Double.MAX_VALUE, 2));
    }

    @Test
    void formatsEffectLevelsBeyondVanillaTranslationRangeAsNumbers() {
        var vanilla = LegendaryTargetEffectTrait.formatAmplifier(5);
        assertTrue(vanilla.getContents() instanceof TranslatableContents);
        assertEquals("potion.potency.5", ((TranslatableContents) vanilla.getContents()).getKey());
        assertEquals("7", LegendaryTargetEffectTrait.formatAmplifier(6).getString());
        assertEquals("20", LegendaryTargetEffectTrait.formatAmplifier(19).getString());
    }

    @Test
    void effectAmplifierMultiplicationSaturatesOnOverflow() {
        assertEquals(19, LegendaryTargetEffectTrait.scaleAmplifier(20, 1));
        assertEquals(Integer.MAX_VALUE,
                LegendaryTargetEffectTrait.scaleAmplifier(20, 120_000_000));
    }

    @Test
    void sealTraitSkipsDeadOrRemovedTargetsBeforeCapabilityAccess() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/l2hostility_tweaks/content/traits/SealTrait.java"));

        String guard = "if (!target.isAlive() || target.isRemoved()) return;";
        assertTrue(source.contains(guard));
        assertTrue(source.indexOf(guard) < source.indexOf("MobTraitCap.HOLDER.isProper(target)"));
    }

    private static DecimalFormat vanillaAttributeFormat() {
        return new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    }
}
