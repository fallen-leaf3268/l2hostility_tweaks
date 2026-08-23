package com.l2hostility_tweaks.content.traits;

import net.minecraft.ChatFormatting;

import java.util.function.IntSupplier;

public final class LegendaryTraitColor {

    private static final IntSupplier GOLD = () -> ChatFormatting.GOLD.getColor();

    private LegendaryTraitColor() {
    }

    public static IntSupplier normalize(IntSupplier color) {
        return color != null ? color : GOLD;
    }
}
