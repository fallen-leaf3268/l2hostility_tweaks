package com.l2hostility_tweaks.proxy;

import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;

import java.util.Map;

/** 客户端/服务端代理接口 — 默认空实现 */
public interface IProxy {

	void receiveSealState(Map<String, Long> remainingTicks);

	default void receiveTraitSpawnIndex(TraitSpawnIndexSnapshot snapshot) {
	}

	default void clearTraitSpawnIndex() {
	}
}
