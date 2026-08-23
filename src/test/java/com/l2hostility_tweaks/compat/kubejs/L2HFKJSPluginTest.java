package com.l2hostility_tweaks.compat.kubejs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class L2HFKJSPluginTest {

	@AfterEach
	void clearFlags() {
		SpellDamageFlags.clear();
	}

	@Test
	void clearsSpellBypassFlagsBeforeScriptReload() throws ReflectiveOperationException {
		Set<Object> activeTags = activeTags();
		activeTags.add(new Object());

		new L2HFKJSPlugin().clearCaches();

		assertFalse(activeTags.iterator().hasNext());
	}

	@SuppressWarnings("unchecked")
	private static Set<Object> activeTags() throws ReflectiveOperationException {
		Field field = SpellDamageFlags.class.getDeclaredField("ACTIVE_TAGS");
		field.setAccessible(true);
		return (Set<Object>) field.get(null);
	}
}
