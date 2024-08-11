package com.michaelsebero.portium.proxy;

/** Client particles that common code asks the proxy to spawn. */
public enum ParticleKind {
    /** Rises in upward bubble columns and pops at the surface. */
    BUBBLE_COLUMN_UP,
    /** Swirls down in whirlpool bubble columns. */
    CURRENT_DOWN,
    /** Drifts from a conduit's frame (or its target) into the conduit; arguments are the start offset. */
    NAUTILUS,
    /** The blue specks trailing a swimming dolphin. */
    DOLPHIN,
    /** The black cloud a hurt squid squirts; arguments are its launch motion. */
    SQUID_INK
}
