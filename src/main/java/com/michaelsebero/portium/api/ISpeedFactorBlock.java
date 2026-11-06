package com.michaelsebero.portium.api;

/**
 * 1.15's block speed and jump factors (honey blocks): standing in or on such a block scales how fast a mob moves
 * across it and how high it jumps. The block a mob stands in counts first, else the one under its feet.
 */
public interface ISpeedFactorBlock {

    default float getSpeedFactor() {
        return 1.0F;
    }

    default float getJumpFactor() {
        return 1.0F;
    }
}
