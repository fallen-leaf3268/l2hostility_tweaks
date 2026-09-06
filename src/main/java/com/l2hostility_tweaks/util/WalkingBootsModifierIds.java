package com.l2hostility_tweaks.util;

import java.util.UUID;

public final class WalkingBootsModifierIds {

    public static final UUID MOVEMENT_SPEED_CAP_ID =
            UUID.fromString("5e096b4f-0c23-47f0-bc93-cb91ce06b379");

    private WalkingBootsModifierIds() {
    }

    public static boolean isMovementSpeedCapModifier(Object candidate) {
        return MOVEMENT_SPEED_CAP_ID.equals(candidate);
    }
}
