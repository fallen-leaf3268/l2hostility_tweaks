package com.l2hostility_tweaks.mixin;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2hostility.content.config.EntityConfig;
import dev.xkmc.l2hostility.content.config.SpecialConfigCondition;
import dev.xkmc.l2library.serial.config.BaseConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityNbtConditionPipelineTest {

	@Test
	void invalidConditionsAreDisabledWithoutMixinInnerTypes() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/EntityConfigMixin.java"));
		String compact = source.replaceAll("\\s+", " ");

		assertFalse(source.contains("enum Decision"));
		assertFalse(source.contains("l2fix$decision"));
		assertTrue(compact.contains("data.l2fix$getNbtConditionState() == " +
				"EntityConfigNbtData.State.NONE"));
		assertTrue(compact.contains("EntityConfigNbtData.State state = " +
				"data.l2fix$getNbtConditionState();"));
		assertTrue(compact.contains("state == EntityConfigNbtData.State.VALID"));
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

		MixinTestInvoker.call(EntityConfigMixin.class,
				"l2fix$removeDisabledFromConditions", buckets, disabled);

		assertEquals(1, buckets.size());
		assertEquals(1, buckets.get(mixedId).size());
		assertEquals(retained, buckets.get(mixedId).get(0).getSecond());
		assertFalse(buckets.containsKey(disabledOnlyId));
	}

	@Test
	void scansResourcesOnlyForEntityConfigMerges() {
		assertFalse(MixinTestInvoker.<Boolean>call(ConfigMergerMixin.class,
				"l2fix$containsEntityConfig", List.of(new BaseConfig())));
		assertTrue(MixinTestInvoker.<Boolean>call(ConfigMergerMixin.class,
				"l2fix$containsEntityConfig", List.of(new EntityConfig())));
		assertTrue(MixinTestInvoker.<Boolean>call(ConfigMergerMixin.class,
				"l2fix$containsEntityConfig", List.of(new BaseConfig(), new EntityConfig())));
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
		assertTrue(classifier.contains("state == EntityConfigNbtData.State.VALID"));
		assertTrue(classifier.contains("l2fix$removeDisabledFromConditions(conditions, config)"));
		assertFalse(merger.contains("java.lang.reflect.Field"));
		assertFalse(classifier.contains("java.lang.reflect.Field"));
		assertFalse(merger.contains("Map<String, Object>"));
		assertFalse(classifier.contains("Map<String, Object>"));
		assertFalse(merger.contains("getAsInt()"));
		assertFalse(merger.contains("nbtStoreBuilt"));
		assertFalse(merger.contains("static volatile Map<ResourceLocation, Map<Integer, NbtEntry>> nbtStore"));
		assertTrue(merger.indexOf("if (!l2fix$containsEntityConfig(list)) return;")
				< merger.indexOf("l2fix$buildNbtStore(server)"));
	}

	@Test
	void readsEntityConfigResourcesWithMinecraftUtf8Reader() throws IOException {
		String merger = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/mixin/ConfigMergerMixin.java"));

		assertTrue(merger.contains("entry.getValue().openAsReader()"));
		assertFalse(merger.contains("new InputStreamReader("));
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
