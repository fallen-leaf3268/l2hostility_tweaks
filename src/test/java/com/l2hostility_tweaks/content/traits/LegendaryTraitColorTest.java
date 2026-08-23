package com.l2hostility_tweaks.content.traits;

import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

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
}
