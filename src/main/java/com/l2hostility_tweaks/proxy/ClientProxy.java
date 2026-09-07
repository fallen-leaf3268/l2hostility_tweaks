package com.l2hostility_tweaks.proxy;

import com.l2hostility_tweaks.client.PlayerTraitScreen;
import com.l2hostility_tweaks.client.TraitSpawnClientCache;
import com.l2hostility_tweaks.generation.view.TraitSpawnIndexSnapshot;
import com.l2hostility_tweaks.network.NetworkHandler;

import java.util.Map;

public class ClientProxy implements IProxy {

	@Override
	public void receiveSealState(Map<String, Long> remainingTicks) {
		PlayerTraitScreen.receiveSealState(remainingTicks);
	}

	@Override
	public void receiveTraitSpawnIndex(TraitSpawnIndexSnapshot snapshot) {
		TraitSpawnClientCache.INSTANCE.install(snapshot);
	}

	@Override
	public void clearTraitSpawnIndex() {
		NetworkHandler.clearTraitSpawnIndexTransport();
		TraitSpawnClientCache.INSTANCE.clear();
	}
}
