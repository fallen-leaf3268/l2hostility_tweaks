package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.EntityConfigNbtData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityNbtConditionPipelineTest {

	@Test
	void invalidConditionsAreDisabledInsteadOfOrdinary() {
		assertEquals(EntityConfigMixin.Decision.ORDINARY,
				EntityConfigMixin.l2fix$decision(EntityConfigNbtData.State.NONE));
		assertEquals(EntityConfigMixin.Decision.CONDITIONAL,
				EntityConfigMixin.l2fix$decision(EntityConfigNbtData.State.VALID));
		assertEquals(EntityConfigMixin.Decision.DISABLED,
				EntityConfigMixin.l2fix$decision(EntityConfigNbtData.State.INVALID));
		assertNotEquals(EntityConfigMixin.Decision.ORDINARY,
				EntityConfigMixin.l2fix$decision(EntityConfigNbtData.State.INVALID));
	}

	@Test
	void pipelineUsesExplicitMetadataWithoutReflectionOrLossyMaps() throws IOException {
		String merger = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/ConfigMergerMixin.java"));
		String classifier = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/EntityConfigMixin.java"));

		assertTrue(merger.contains("EntityConfigNbtData"));
		assertTrue(merger.contains("l2fix$setNbtCondition"));
		assertTrue(classifier.contains("EntityConfigNbtData"));
		assertTrue(classifier.contains("case INVALID -> Decision.DISABLED"));
		assertFalse(merger.contains("java.lang.reflect.Field"));
		assertFalse(classifier.contains("java.lang.reflect.Field"));
		assertFalse(merger.contains("Map<String, Object>"));
		assertFalse(classifier.contains("Map<String, Object>"));
		assertFalse(merger.contains("getAsInt()"));
	}
}
