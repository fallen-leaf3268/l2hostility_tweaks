package com.l2hostility_tweaks.mixin;

import com.l2hostility_tweaks.util.EntityConfigNbtData;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.config.SpecialConfigCondition;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
	void removesDisabledConfigFromEveryConditionBucket() {
		EntityConfig.Config disabled = new EntityConfig.Config();
		EntityConfig.Config retained = new EntityConfig.Config();
		Map<ResourceLocation, ArrayList<Pair<SpecialConfigCondition<?>, EntityConfig.Config>>> buckets =
				new LinkedHashMap<>();
		ResourceLocation mixedId = new ResourceLocation("test", "mixed");
		ResourceLocation disabledOnlyId = new ResourceLocation("test", "disabled_only");
		buckets.put(mixedId, pairs(disabled, retained, disabled));
		buckets.put(disabledOnlyId, pairs(disabled));

		EntityConfigMixin.l2fix$removeDisabledFromConditions(buckets, disabled);

		assertEquals(1, buckets.size());
		assertEquals(1, buckets.get(mixedId).size());
		assertEquals(retained, buckets.get(mixedId).get(0).getSecond());
		assertFalse(buckets.containsKey(disabledOnlyId));
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

	private static ArrayList<Pair<SpecialConfigCondition<?>, EntityConfig.Config>> pairs(
			EntityConfig.Config... configs) {
		ArrayList<Pair<SpecialConfigCondition<?>, EntityConfig.Config>> result = new ArrayList<>();
		for (EntityConfig.Config config : configs) {
			result.add(Pair.of(null, config));
		}
		return result;
	}
}
