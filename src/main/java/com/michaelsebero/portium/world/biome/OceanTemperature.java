package com.michaelsebero.portium.world.biome;

/** The five 1.13 ocean temperatures. */
public enum OceanTemperature {

    WARM,
    LUKEWARM,
    NORMAL,
    COLD,
    FROZEN;

    /** 1.13's thresholds on the ocean temperature noise. */
    public static OceanTemperature fromNoise(double value) {
        if (value > 0.4D) return WARM;
        if (value > 0.2D) return LUKEWARM;
        if (value < -0.4D) return FROZEN;
        if (value < -0.2D) return COLD;
        return NORMAL;
    }
}
