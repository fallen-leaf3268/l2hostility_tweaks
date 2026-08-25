package com.l2hostility_tweaks.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

final class TraitTextLayout {

	record Entry<O, S>(O owner, S text) {}

	record Segment<O, T>(O owner, T text, int xOffset, int width) {}

	static <O, S, T> List<List<Segment<O, T>>> layout(
			List<Entry<O, S>> entries, int maxWidth, T separator,
			ToIntFunction<T> width, BiFunction<S, Integer, List<T>> split) {
		int limit = Math.max(1, maxWidth);
		int separatorWidth = width.applyAsInt(separator);
		List<List<Segment<O, T>>> rows = new ArrayList<>();
		List<Segment<O, T>> current = new ArrayList<>();
		int currentWidth = 0;

		for (Entry<O, S> entry : entries) {
			List<T> fragments = split.apply(entry.text(), limit).stream()
					.filter(fragment -> width.applyAsInt(fragment) > 0).toList();
			if (fragments.isEmpty()) continue;

			if (fragments.size() > 1) {
				if (!current.isEmpty()) rows.add(List.copyOf(current));
				current = new ArrayList<>();
				currentWidth = 0;
				for (T fragment : fragments) {
					int fragmentWidth = width.applyAsInt(fragment);
					rows.add(List.of(new Segment<>(entry.owner(), fragment, 0, fragmentWidth)));
				}
				continue;
			}

			T fragment = fragments.get(0);
			int fragmentWidth = width.applyAsInt(fragment);
			int needed = current.isEmpty() ? fragmentWidth : separatorWidth + fragmentWidth;
			if (!current.isEmpty() && currentWidth + needed > limit) {
				rows.add(List.copyOf(current));
				current = new ArrayList<>();
				currentWidth = 0;
			}
			if (!current.isEmpty()) {
				current.add(new Segment<>(null, separator, currentWidth, separatorWidth));
				currentWidth += separatorWidth;
			}
			current.add(new Segment<>(entry.owner(), fragment, currentWidth, fragmentWidth));
			currentWidth += fragmentWidth;
		}
		if (!current.isEmpty()) rows.add(List.copyOf(current));
		return List.copyOf(rows);
	}

	private TraitTextLayout() {}
}
