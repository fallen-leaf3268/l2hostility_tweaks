package com.l2hostility_tweaks.util;

public final class TraitCostHelper {

	public static final int UNPAYABLE = -1;

	private TraitCostHelper() {
	}

	public static int upgradeCost(int mode, int currentLevel, int maxStackSize) {
		if (currentLevel < 0 || maxStackSize <= 0) return UNPAYABLE;
		if (mode == 2) return saturate((long) currentLevel + 1);
		if (mode != 3) return 1;
		int largestPower = Integer.highestOneBit(maxStackSize);
		int maxExponent = Integer.numberOfTrailingZeros(largestPower);
		return currentLevel <= maxExponent ? 1 << currentLevel : UNPAYABLE;
	}

	public static int singleRefund(int mode, int currentLevel, int maxStackSize) {
		long level = Math.abs((long) currentLevel);
		if (level == 0 || maxStackSize <= 0) return 0;
		if (mode == 2) return saturate(level);
		if (mode != 3) return 1;
		int largestPower = Integer.highestOneBit(maxStackSize);
		int maxExponent = Integer.numberOfTrailingZeros(largestPower);
		return level - 1 <= maxExponent ? 1 << (int) (level - 1) : largestPower;
	}

	public static int totalRefund(int mode, int currentLevel, int maxStackSize) {
		long level = Math.abs((long) currentLevel);
		if (level == 0 || maxStackSize <= 0) return 0;
		if (mode == 2) return saturate(level * (level + 1) / 2);
		if (mode != 3) return saturate(level);
		int largestPower = Integer.highestOneBit(maxStackSize);
		int payableLevels = Math.min((int) Math.min(level, Integer.MAX_VALUE),
				Integer.numberOfTrailingZeros(largestPower) + 1);
		return (int) ((1L << payableLevels) - 1);
	}

	private static int saturate(long value) {
		return (int) Math.min(value, Integer.MAX_VALUE);
	}
}
