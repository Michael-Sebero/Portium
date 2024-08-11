package com.michaelsebero.portium.command;

/**
 * A pair of floats: a rotation (pitch, yaw) or a horizontal position (x, z). 1.12's own {@code Vec2} exists only
 * on the client, so commands on a dedicated server can't use it.
 */
public final class Vec2 {

    public static final Vec2 ZERO = new Vec2(0.0F, 0.0F);

    public final float x;
    public final float y;

    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
