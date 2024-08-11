package com.michaelsebero.portium.world.v116;

/** How 1.16-style features set blocks in a 1.12 world. */
public final class GenFlags {

    /**
     * World generation: sent to clients, with no neighbour or observer updates. 1.16 places generated blocks without
     * any, and 1.12's observer check on a chunk's edge would look into the next chunk, making it generate.
     */
    public static final int GENERATION = 2 | 16;

    /** Growth from bone meal: sent to clients, with the neighbour shape updates 1.16 gives it. */
    public static final int GROWTH = 2;

    private GenFlags() {}
}
