package com.l2hostility_tweaks.util;

import com.google.gson.JsonObject;

public interface EntityConfigNbtData {

	enum State {
		NONE,
		VALID,
		INVALID
	}

	void l2fix$setNbtCondition(State state, JsonObject condition);

	State l2fix$getNbtConditionState();

	JsonObject l2fix$getNbtCondition();
}
