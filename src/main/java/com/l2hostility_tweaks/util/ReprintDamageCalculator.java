package com.l2hostility_tweaks.util;

public final class ReprintDamageCalculator {

	private ReprintDamageCalculator() {
	}

	public static double counterReduction(int level, double reductionPerLevel) {
		return Math.min(level * reductionPerLevel, 0.8);
	}

	public static Result calculate(boolean linear, Iterable<Point> points) {
		long total = 0;
		int maxLevel = 0;
		int counterTotal = 0;
		boolean highLevel = false;

		for (Point point : points) {
			int level = point.level();
			if (point.counter()) {
				counterTotal += level;
				if (linear) {
					total -= level;
				} else if (!highLevel) {
					total -= 1L << (level - 1);
				}
				continue;
			}

			maxLevel = Math.max(maxLevel, level);
			if (linear) {
				total += level;
			} else if (level >= 30) {
				highLevel = true;
			} else if (!highLevel) {
				total += 1L << (level - 1);
			}
		}

		if (linear) {
			return new Result((float) Math.max(0, total), maxLevel);
		}
		if (highLevel) {
			int exponent = Math.max(0, maxLevel - 1 - counterTotal);
			return new Result((float) Math.pow(2, exponent), maxLevel);
		}
		return new Result((float) Math.max(0, total), maxLevel);
	}

	public record Point(int level, boolean counter) {
	}

	public record Result(float factor, int maxLevel) {
	}
}
