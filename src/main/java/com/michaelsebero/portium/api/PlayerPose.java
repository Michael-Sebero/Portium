package com.michaelsebero.portium.api;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.entity.player.EntityPlayer;

/**
 * A player's body pose (1.14). It decides the hitbox and eye height; SWIMMING is also used for crawling.
 */
public enum PlayerPose {

    STANDING,
    CROUCHING,
    SWIMMING,
    FALL_FLYING,
    SPIN_ATTACK,
    SLEEPING;

    public float getWidth() {
        return this == SLEEPING ? 0.2F : 0.6F;
    }

    public float getHeight() {
        switch (this) {
            case CROUCHING: return PortiumConfig.mechanics.shorterSneaking ? 1.5F : 1.65F;
            case SWIMMING:
            case FALL_FLYING:
            case SPIN_ATTACK: return 0.6F;
            case SLEEPING: return 0.2F;
            default: return 1.8F;
        }
    }

    public float getEyeHeight(EntityPlayer player) {
        switch (this) {
            case CROUCHING: return PortiumConfig.mechanics.shorterSneaking ? 1.27F : player.eyeHeight - 0.08F;
            case SWIMMING:
            case FALL_FLYING:
            case SPIN_ATTACK: return 0.4F;
            case SLEEPING: return 0.2F;
            default: return player.eyeHeight;
        }
    }
}
