package com.l2hostility_tweaks.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitTextLayoutTest {

	record StyledText(String value, String style) {}

	private static List<String> split(String text, int width) {
		List<String> result = new ArrayList<>();
		for (int start = 0; start < text.length(); start += width)
			result.add(text.substring(start, Math.min(text.length(), start + width)));
		return result;
	}

	@Test
	void packsNormalEntriesThroughExactBoundary() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("first", "aa"),
				new TraitTextLayout.Entry<>("second", "bb")),
				5, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(1, rows.size());
		assertEquals(List.of("aa", " ", "bb"),
				rows.get(0).stream().map(TraitTextLayout.Segment::text).toList());
		assertEquals(List.of(0, 2, 3),
				rows.get(0).stream().map(TraitTextLayout.Segment::xOffset).toList());
	}

	@Test
	void splitsFirstOversizedEntryWithoutLeadingEmptyRow() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("trait", "abcdef")),
				3, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(2, rows.size());
		assertEquals(List.of("abc", "def"),
				rows.stream().map(row -> row.get(0).text()).toList());
		assertTrue(rows.stream().noneMatch(List::isEmpty));
		assertTrue(rows.stream().flatMap(List::stream).allMatch(segment -> segment.width() <= 3));
		assertTrue(rows.stream().flatMap(List::stream).allMatch(segment -> segment.owner().equals("trait")));
	}

	@Test
	void flushesNormalRowBeforeOversizedEntryAndPreservesOrder() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("short", "ab"),
				new TraitTextLayout.Entry<>("long", "1234567")),
				3, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(List.of("ab", "123", "456", "7"),
				rows.stream().map(row -> row.get(0).text()).toList());
		assertEquals(List.of("short", "long", "long", "long"),
				rows.stream().map(row -> row.get(0).owner()).toList());
	}

	@Test
	void ignoresEmptySplitResultsAndClampsInvalidWidth() {
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("empty", ""),
				new TraitTextLayout.Entry<>("visible", "x")),
				0, " ", String::length, TraitTextLayoutTest::split);

		assertEquals(1, rows.size());
		assertEquals("x", rows.get(0).get(0).text());
	}

	@Test
	void preservesStyledFragmentsAndOwnerForInteraction() {
		StyledText first = new StyledText("abc", "gray-strikethrough");
		StyledText second = new StyledText("def", "gray-strikethrough");
		var rows = TraitTextLayout.layout(List.of(
				new TraitTextLayout.Entry<>("sealed", "source")),
				3, new StyledText(" ", "plain"), text -> text.value().length(),
				(text, width) -> List.of(first, second));

		assertSame(first, rows.get(0).get(0).text());
		assertSame(second, rows.get(1).get(0).text());
		assertEquals("sealed", rows.get(0).get(0).owner());
		assertEquals("sealed", rows.get(1).get(0).owner());
	}
}
