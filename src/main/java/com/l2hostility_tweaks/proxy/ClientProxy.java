package com.l2hostility_tweaks.proxy;

import com.l2hostility_tweaks.client.PlayerTraitScreen;

import java.util.Map;

public class ClientProxy implements IProxy {

	@Override
	public void receiveSealState(Map<String, Long> remainingTicks) {
		PlayerTraitScreen.receiveSealState(remainingTicks);
	}
}
