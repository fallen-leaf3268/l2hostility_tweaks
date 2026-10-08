package com.l2hostility_tweaks.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record TraitListTooltip(List<Entry> entries, boolean showIcons, boolean replaceTooltip,
                               boolean overheadLayout)
        implements TooltipComponent {

    private static final String MARKER_PREFIX = "\u0000l2ht_trait_list:";

    public TraitListTooltip {
        entries = List.copyOf(entries);
    }

    public static Component marker(List<ResourceLocation> traitIds) {
        return encodedMarker(traitIds.stream().map(id -> new Entry(id, null)).toList(), 'i');
    }

    public static Component exclusiveMarker(List<ResourceLocation> traitIds) {
        return encodedMarker(traitIds.stream().map(id -> new Entry(id, null)).toList(), 'x');
    }

    public static Component textMarker(List<Entry> entries) {
        return encodedMarker(entries, 't');
    }

    public static Component overheadMarker(List<Entry> entries) {
        return encodedMarker(entries, 'h');
    }

    public static Optional<TraitListTooltip> fromMarker(String text) {
        if (!text.startsWith(MARKER_PREFIX)) return Optional.empty();
        String encoded = text.substring(MARKER_PREFIX.length());
        if (encoded.length() < 2 || encoded.charAt(1) != ':') return Optional.empty();
        char mode = encoded.charAt(0);
        boolean showIcons = mode == 'i' || mode == 'x';
        boolean replaceTooltip = mode == 'x';
        boolean overheadLayout = mode == 'h';
        if (!showIcons && mode != 't' && !overheadLayout) return Optional.empty();
        String entriesText = encoded.substring(2);
        if (entriesText.isEmpty()) {
            return Optional.of(new TraitListTooltip(
                    List.of(), showIcons, replaceTooltip, overheadLayout));
        }
        List<Entry> entries = Arrays.stream(entriesText.split(","))
                .map(TraitListTooltip::parseEntry)
                .flatMap(Optional::stream)
                .toList();
        return Optional.of(new TraitListTooltip(entries, showIcons, replaceTooltip, overheadLayout));
    }

    public List<List<Entry>> rows() {
        if (!overheadLayout) return entries.stream().map(List::of).toList();
        List<List<Entry>> rows = new ArrayList<>();
        for (int start = 0; start < entries.size(); start += 3) {
            rows.add(List.copyOf(entries.subList(start, Math.min(start + 3, entries.size()))));
        }
        return List.copyOf(rows);
    }

    private static Component encodedMarker(List<Entry> entries, char mode) {
        String joined = entries.stream().map(entry -> entry.traitId()
                        + (entry.rank() == null ? "" : "@" + entry.rank()))
                .collect(java.util.stream.Collectors.joining(","));
        return Component.literal(MARKER_PREFIX + mode + ":" + joined);
    }

    private static Optional<Entry> parseEntry(String encoded) {
        int separator = encoded.lastIndexOf('@');
        String idText = separator < 0 ? encoded : encoded.substring(0, separator);
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) return Optional.empty();
        if (separator < 0) return Optional.of(new Entry(id, null));
        try {
            return Optional.of(new Entry(id, Integer.parseInt(encoded.substring(separator + 1))));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public record Entry(ResourceLocation traitId, Integer rank) {
    }
}
