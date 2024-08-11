package com.michaelsebero.portium.world.v116.noise;

/** Two octave noises added up, one sampled slightly stretched (1.16's MaxMinNoiseMixer, behind its biome sources). */
public final class NormalNoise {

    private static final double STRETCH = 1.0181268882175227D;

    private final double valueFactor;
    private final OctaveNoise first;
    private final OctaveNoise second;

    public NormalNoise(SeedRandom random, int firstOctave, double[] amplitudes) {
        this.first = new OctaveNoise(random, firstOctave, amplitudes);
        this.second = new OctaveNoise(random, firstOctave, amplitudes);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] != 0.0D) {
                min = Math.min(min, i);
                max = Math.max(max, i);
            }
        }
        this.valueFactor = 0.16666666666666666D / (0.1D * (1.0D + 1.0D / (double) (max - min + 1)));
    }

    public double noise(double x, double y, double z) {
        return (this.first.noise(x, y, z) + this.second.noise(x * STRETCH, y * STRETCH, z * STRETCH)) * this.valueFactor;
    }
}
