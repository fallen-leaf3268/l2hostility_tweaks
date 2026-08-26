package com.l2hostility_tweaks.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitCostHelperTest {

	@Test
	void exponentialUpgradeCostsStopAtSingleStackLimit() {
		int[] expected = {1, 2, 4, 8, 16, 32, 64};
		for (int level = 0; level < expected.length; level++) {
			assertEquals(expected[level], TraitCostHelper.upgradeCost(3, level, 64));
		}
		assertEquals(TraitCostHelper.UNPAYABLE, TraitCostHelper.upgradeCost(3, 7, 64));
		assertEquals(TraitCostHelper.UNPAYABLE, TraitCostHelper.upgradeCost(3, 31, 64));
		assertEquals(TraitCostHelper.UNPAYABLE, TraitCostHelper.upgradeCost(3, 32, 64));
	}

	@Test
	void invalidUpgradeInputsAreUnpayable() {
		assertEquals(TraitCostHelper.UNPAYABLE, TraitCostHelper.upgradeCost(3, -1, 64));
		assertEquals(TraitCostHelper.UNPAYABLE, TraitCostHelper.upgradeCost(3, 0, 0));
		assertEquals(TraitCostHelper.UNPAYABLE, TraitCostHelper.upgradeCost(3, 0, -1));
	}

	@Test
	void exponentialSingleRefundStopsAtLastPayableStage() {
		assertEquals(1, TraitCostHelper.singleRefund(3, 1, 64));
		assertEquals(2, TraitCostHelper.singleRefund(3, 2, 64));
		assertEquals(64, TraitCostHelper.singleRefund(3, 7, 64));
		assertEquals(64, TraitCostHelper.singleRefund(3, 31, 64));
		assertEquals(64, TraitCostHelper.singleRefund(3, 32, 64));
	}

	@Test
	void exponentialTotalRefundStopsAtReachableSchedule() {
		assertEquals(1, TraitCostHelper.totalRefund(3, 1, 64));
		assertEquals(3, TraitCostHelper.totalRefund(3, 2, 64));
		assertEquals(127, TraitCostHelper.totalRefund(3, 7, 64));
		assertEquals(127, TraitCostHelper.totalRefund(3, 31, 64));
		assertEquals(127, TraitCostHelper.totalRefund(3, 32, 64));
	}

	@Test
	void nonPositiveRefundInputsReturnZero() {
		assertEquals(0, TraitCostHelper.singleRefund(3, 0, 64));
		assertEquals(0, TraitCostHelper.singleRefund(3, 1, 0));
		assertEquals(0, TraitCostHelper.totalRefund(3, 0, 64));
		assertEquals(0, TraitCostHelper.totalRefund(3, 1, 0));
	}

	@Test
	void existingConstantAndLinearSchedulesRemainUnchanged() {
		assertEquals(1, TraitCostHelper.upgradeCost(1, 5, 64));
		assertEquals(1, TraitCostHelper.singleRefund(1, 5, 64));
		assertEquals(5, TraitCostHelper.totalRefund(1, 5, 64));
		assertEquals(6, TraitCostHelper.upgradeCost(2, 5, 64));
		assertEquals(5, TraitCostHelper.singleRefund(2, 5, 64));
		assertEquals(15, TraitCostHelper.totalRefund(2, 5, 64));
	}
}
