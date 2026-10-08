package com.l2hostility_tweaks.client;

final class DamageTrailAnimation {

    private static final long HOLD_MILLIS = 400;
    private static final double BASE_DRAIN = 0.3;
    private static final double DRAIN_SCALE = 2.5;

    private int trackedId = -1;
    private float actualFraction = 1f;
    private double damageTrail;
    private long lastDamageMillis;
    private long lastUpdateMillis;

    float update(int entityId, float healthFraction, long nowMillis) {
        if (entityId != trackedId) {
            trackedId = entityId;
            actualFraction = healthFraction;
            damageTrail = 0;
            lastUpdateMillis = nowMillis;
            return healthFraction;
        }
        long currentMillis = Math.max(nowMillis, lastUpdateMillis);
        double remainingDamage = remainingDamage(currentMillis);
        if (healthFraction < actualFraction - 0.001f) {
            damageTrail = remainingDamage + actualFraction - healthFraction;
            lastDamageMillis = currentMillis;
            remainingDamage = damageTrail;
        }
        actualFraction = healthFraction;
        lastUpdateMillis = currentMillis;
        return (float) Math.min(1.0, actualFraction + remainingDamage);
    }

    private double remainingDamage(long nowMillis) {
        long elapsed = nowMillis - lastDamageMillis;
        if (elapsed <= HOLD_MILLIS) return damageTrail;
        double seconds = (elapsed - HOLD_MILLIS) / 1000.0;
        double baseline = BASE_DRAIN / DRAIN_SCALE;
        return Math.max(0, (damageTrail + baseline) * Math.exp(-DRAIN_SCALE * seconds) - baseline);
    }
}
