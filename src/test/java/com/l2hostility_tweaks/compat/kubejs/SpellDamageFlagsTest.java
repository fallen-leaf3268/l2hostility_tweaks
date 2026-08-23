package com.l2hostility_tweaks.compat.kubejs;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellDamageFlagsTest {

	private RecordingAppender appender;

	@BeforeEach
	void attachAppender() {
		SpellDamageFlags.clear();
		appender = new RecordingAppender();
		appender.start();
		logger().addAppender(appender);
	}

	@AfterEach
	void detachAppender() {
		logger().removeAppender(appender);
		appender.stop();
		SpellDamageFlags.clear();
	}

	@Test
	void rejectsNullTypeWithoutChangingFlags() {
		assertDoesNotThrow(() -> SpellDamageFlags.enableBypass(null));
		assertTrue(activeTags().isEmpty());
		assertInvalidTypeLogged("null");
	}

	@Test
	void rejectsUnknownTypeWithoutChangingFlags() {
		assertDoesNotThrow(() -> SpellDamageFlags.enableBypass("bypass_resistence"));
		assertTrue(activeTags().isEmpty());
		assertInvalidTypeLogged("bypass_resistence");
	}

	private void assertInvalidTypeLogged(String value) {
		assertTrue(appender.events.stream().anyMatch(event -> {
			String message = event.getMessage().getFormattedMessage();
			return event.getLevel() == Level.ERROR && message.contains(value) &&
					message.contains("bypass_armor") && message.contains("bypass_resistance");
		}));
	}

	private static Logger logger() {
		return (Logger) LogManager.getLogger("l2htweaks:kubejs");
	}

	@SuppressWarnings("unchecked")
	private static Set<Object> activeTags() {
		try {
			Field field = SpellDamageFlags.class.getDeclaredField("ACTIVE_TAGS");
			field.setAccessible(true);
			return (Set<Object>) field.get(null);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError(exception);
		}
	}

	private static final class RecordingAppender extends AbstractAppender {

		private final List<LogEvent> events = new ArrayList<>();

		private RecordingAppender() {
			super("recording", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}
}
