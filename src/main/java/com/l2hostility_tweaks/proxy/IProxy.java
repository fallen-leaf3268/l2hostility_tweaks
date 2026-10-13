package com.l2hostility_tweaks.proxy;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;

import java.util.Map;

public interface IProxy {

	void receiveSealState(Map<String, Long> remainingTicks);

	default void receiveTraitSpawnIndex(TraitSpawnIndexSnapshot snapshot) {
	}

	default void clearTraitSpawnIndex() {
	}
}
