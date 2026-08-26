package com.l2hostility_tweaks.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l2hostility_tweaks.util.EntityConfigNbtData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EntityConfigConfigMixinTest {

	@Test
	void defaultsToNoneAndPreservesExplicitStates() {
		EntityConfigNbtData data = new EntityConfigConfigMixin();
		assertEquals(EntityConfigNbtData.State.NONE, data.l2fix$getNbtConditionState());
		assertNull(data.l2fix$getNbtCondition());

		JsonObject condition = JsonParser.parseString("{\"elite\":true}").getAsJsonObject();
		data.l2fix$setNbtCondition(EntityConfigNbtData.State.VALID, condition);
		assertEquals(EntityConfigNbtData.State.VALID, data.l2fix$getNbtConditionState());
		assertSame(condition, data.l2fix$getNbtCondition());

		data.l2fix$setNbtCondition(EntityConfigNbtData.State.INVALID, null);
		assertEquals(EntityConfigNbtData.State.INVALID, data.l2fix$getNbtConditionState());
		assertNull(data.l2fix$getNbtCondition());
	}
}
