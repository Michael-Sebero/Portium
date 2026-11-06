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
    SQUID_INK,
    /** The blue flame of soul torches and soul fire (1.16). */
    SOUL_FIRE_FLAME,
    /** A campfire's smoke (1.14); arguments are its motion. Shown at any distance, as 1.14 forces it. */
    CAMPFIRE_COSY_SMOKE,
    /** A signal fire's tall smoke column (a campfire on hay, 1.14). Shown at any distance. */
    CAMPFIRE_SIGNAL_SMOKE,
    /** Rises from charged respawn anchors (1.16). */
    REVERSE_PORTAL,
    /** The specks of a composter being filled (1.14). */
    COMPOSTER,
    /** A panda's sneeze (1.14): a green puff of cloud; arguments are its motion. */
    SNEEZE,
    /** A wisp of soul (1.16): from soul speed boots on soul sand, and soul fire; arguments are its motion. */
    SOUL,
    /** Honey dripping from under a full beehive (1.15). */
    DRIPPING_HONEY,
    /** Nectar falling from a pollen-laden bee (1.15). */
    FALLING_NECTAR;

    /** Whether 1.12's particle distance (32 blocks) and the Particles setting leave it out, as for its own particles. */
    public boolean limited() {
        return this == SOUL_FIRE_FLAME || this == REVERSE_PORTAL || this == COMPOSTER || this == DRIPPING_HONEY || this == FALLING_NECTAR;
    }
}
