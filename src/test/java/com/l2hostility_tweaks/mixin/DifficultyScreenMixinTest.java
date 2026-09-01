package com.l2hostility_tweaks.mixin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DifficultyScreenMixinTest {

	@Test
	void replacesRankCapSafelyForEveryCustomEntryCombination() {
		assertEntries(null, null, List.of("level", "experience", "rank"));
		assertEntries("custom-level-cap", null,
				List.of("level", "experience", "custom-level-cap"));
		assertEntries(null, "legendary-cap",
				List.of("level", "experience", "legendary-cap"));
		assertEntries("custom-level-cap", "legendary-cap",
				List.of("level", "experience", "custom-level-cap", "legendary-cap"));
	}

	@Test
	void insertsCustomEntriesBeforeChunkInformation() {
		var entries = new ArrayList<>(List.of("level", "experience", "rank", "chunk"));

		MixinTestInvoker.call(DifficultyScreenMixin.class, "l2fix$replaceRankCapEntries",
				entries, "custom-level-cap", "legendary-cap");

		assertEquals(List.of(
				"level", "experience", "custom-level-cap", "legendary-cap", "chunk"), entries);
	}

	private static void assertEntries(String levelCap, String legendaryCap,
			List<String> expected) {
		var entries = new ArrayList<>(List.of("level", "experience", "rank"));
		MixinTestInvoker.call(DifficultyScreenMixin.class, "l2fix$replaceRankCapEntries",
				entries, levelCap, legendaryCap);
		assertEquals(expected, entries);
	}
}
