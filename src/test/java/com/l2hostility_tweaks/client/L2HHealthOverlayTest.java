package com.l2hostility_tweaks.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class L2HHealthOverlayTest {

	@Test
	void resolvesHudAndBossbarVisibilityFromOneStateMatrix() {
		assertEquals(new L2HHealthOverlay.HudState(false, false),
				L2HHealthOverlay.l2fix$resolveHudState(false, false, false));
		assertEquals(new L2HHealthOverlay.HudState(true, true),
				L2HHealthOverlay.l2fix$resolveHudState(true, false, true));
		assertEquals(new L2HHealthOverlay.HudState(false, false),
				L2HHealthOverlay.l2fix$resolveHudState(true, true, true));
		assertEquals(new L2HHealthOverlay.HudState(true, true),
				L2HHealthOverlay.l2fix$resolveHudState(true, true, false));
	}
}
