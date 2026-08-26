package com.l2hostility_tweaks.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitUnloaderWandSafetyTest {

	@Test
	void refundValidationPrecedesTraitMutation() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/content/TraitUnloaderWand.java"));

		int single = source.indexOf("public static void unloadSingleTrait");
		int group = source.indexOf("public static void unloadGroupTrait");
		int instances = source.indexOf("public TraitUnloaderWand");
		String singleBody = source.substring(single, group);
		String groupBody = source.substring(group, instances);
		assertTrue(singleBody.indexOf("TraitWandHelper.isSafeDelivery")
				< singleBody.indexOf("cap.traits.put"));
		assertTrue(groupBody.indexOf("TraitWandHelper.isSafeDelivery")
				< groupBody.indexOf("cap.traits.remove"));
		assertTrue(source.contains("if (!l2fix$canDeliverAllRefunds(entries))"));
	}

	@Test
	void storedLevelsUseOverflowSafeNormalization() throws IOException {
		String unloader = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/content/TraitUnloaderWand.java"));
		String network = Files.readString(Path.of(
				"src/main/java/com/l2hostility_tweaks/network/NetworkHandler.java"));

		assertTrue(unloader.contains("TraitCostHelper.normalizeStoredLevel(currentLevel)"));
		assertTrue(unloader.contains("TraitCostHelper.normalizeStoredLevel(entry.getValue())"));
		assertTrue(network.contains("TraitCostHelper.normalizeStoredLevel(currentLevel)"));
	}
}
