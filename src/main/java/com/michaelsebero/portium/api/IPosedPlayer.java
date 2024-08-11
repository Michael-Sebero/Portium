package com.michaelsebero.portium.api;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Implemented on {@link EntityPlayer} by mixin: 1.13 swimming and 1.14 poses (crouching under low ceilings,
 * crawling through one-block gaps).
 */
public interface IPosedPlayer {

    PlayerPose portium$getPose();

    /** Sprint-swimming in water; synced to clients. */
    boolean portium$isSwimming();

    /** Whether the player's eyes are under water, which is required to start swimming. */
    boolean portium$canSwim();

    /** In the swimming pose, whether swimming in water or crawling on land. */
    default boolean portium$isActuallySwimming() {
        return this.portium$getPose() == PlayerPose.SWIMMING;
    }

    /** Blend from 0 (upright) to 1 (horizontal) for the swimming and crawling animation. */
    float portium$getSwimAnimation(float partialTicks);

    static IPosedPlayer of(EntityPlayer player) {
        return (IPosedPlayer) player;
    }
}
