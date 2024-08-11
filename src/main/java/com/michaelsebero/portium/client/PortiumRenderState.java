package com.michaelsebero.portium.client;

/** Render-time flags shared between client mixins. */
public final class PortiumRenderState {

    /** Set while the first-person arm is drawn, whose pose must not get swimming or spear animations. */
    public static boolean renderingFirstPersonArm;

    private PortiumRenderState() {}
}
