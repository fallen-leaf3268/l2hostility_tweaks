package com.l2hostility_tweaks.client;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class TraitListTooltipRenderer implements ClientTooltipComponent {

    private static final int ROWS_PER_COLUMN = 9;
    private static final int ROW_HEIGHT = 18;
    private static final int ICON_TEXT_GAP = 2;
    private static final int COLUMN_GAP = 6;

    private final List<RenderEntry> entries;
    private final boolean showIcons;

    public TraitListTooltipRenderer(TraitListTooltip tooltip) {
        List<RenderEntry> resolved = new ArrayList<>();
        for (var entry : tooltip.entries()) {
            MobTrait trait = LHTraits.TRAITS.get().getValue(entry.traitId());
            if (trait != null) {
                resolved.add(new RenderEntry(new ItemStack(trait.asItem()), trait.getFullDesc(entry.rank())));
            }
        }
        entries = List.copyOf(resolved);
        showIcons = tooltip.showIcons();
    }

    @Override
    public int getHeight() {
        return Math.min(entries.size(), ROWS_PER_COLUMN) * rowHeight();
    }

    @Override
    public int getWidth(Font font) {
        int columns = columnCount();
        int width = 0;
        for (int column = 0; column < columns; column++) {
            width += columnWidth(font, column);
            if (column + 1 < columns) width += COLUMN_GAP;
        }
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        int columnX = x;
        for (int column = 0; column < columnCount(); column++) {
            int start = column * ROWS_PER_COLUMN;
            int end = Math.min(start + ROWS_PER_COLUMN, entries.size());
            for (int index = start; index < end; index++) {
                RenderEntry entry = entries.get(index);
                int rowY = y + (index - start) * rowHeight();
                int textX = columnX;
                int textY = rowY + 1;
                if (showIcons) {
                    guiGraphics.renderItem(entry.stack(), columnX, rowY);
                    textX += 16 + ICON_TEXT_GAP;
                    textY = rowY + 4;
                }
                guiGraphics.drawString(font, entry.name(), textX, textY, 0xFFFFFFFF, false);
            }
            columnX += columnWidth(font, column) + COLUMN_GAP;
        }
    }

    private int columnCount() {
        return (entries.size() + ROWS_PER_COLUMN - 1) / ROWS_PER_COLUMN;
    }

    private int columnWidth(Font font, int column) {
        int start = column * ROWS_PER_COLUMN;
        int end = Math.min(start + ROWS_PER_COLUMN, entries.size());
        int textWidth = 0;
        for (int index = start; index < end; index++) {
            textWidth = Math.max(textWidth, font.width(entries.get(index).name()));
        }
        return (showIcons ? 16 + ICON_TEXT_GAP : 0) + textWidth;
    }

    private int rowHeight() {
        return showIcons ? ROW_HEIGHT : 11;
    }

    private record RenderEntry(ItemStack stack, Component name) {
    }
}
