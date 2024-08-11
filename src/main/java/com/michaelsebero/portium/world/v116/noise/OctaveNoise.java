package com.michaelsebero.portium.world.v116.noise;

import javax.annotation.Nullable;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Several octaves of {@link ImprovedNoise} added up (1.16's OctavesNoiseGenerator). Octaves are numbered from the
 * finest (0) down to coarser negative ones; each is either given by a list of octave numbers or by a first octave and
 * a weight per octave. The order octaves take their random numbers in, skipped ones included, matches 1.16, so the
 * same seed gives the same noise.
 */
public final class OctaveNoise {

    private final ImprovedNoise[] octaves;
    private final double[] amplitudes;
    private final double lowestFrequency;
    private final double highestAmplitude;

    /** The octaves with these numbers, each at full weight. */
    public static OctaveNoise ofOctaves(SeedRandom random, int... octaveNumbers) {
        SortedSet<Integer> set = new TreeSet<>();
        for (int octave : octaveNumbers) set.add(octave);
        int first = set.first();
        int count = set.last() - first + 1;
        double[] amplitudes = new double[count];
        for (int octave : set) amplitudes[octave - first] = 1.0D;
        return new OctaveNoise(random, first, amplitudes);
    }

    /** Octaves {@code from} to {@code to}, at full weight. */
    public static OctaveNoise ofRange(SeedRandom random, int from, int to) {
        int[] octaves = new int[to - from + 1];
        for (int i = 0; i < octaves.length; i++) octaves[i] = from + i;
        return ofOctaves(random, octaves);
    }

    /** Octaves from {@code firstOctave} up, weighted by {@code amplitudes} (0 skips one). */
    public OctaveNoise(SeedRandom random, int firstOctave, double[] amplitudes) {
        this.amplitudes = amplitudes;
        int count = amplitudes.length;
        int zeroIndex = -firstOctave;
        this.octaves = new ImprovedNoise[count];
        ImprovedNoise first = new ImprovedNoise(random);
        if (zeroIndex >= 0 && zeroIndex < count && amplitudes[zeroIndex] != 0.0D) this.octaves[zeroIndex] = first;
        for (int i = zeroIndex - 1; i >= 0; i--) {
            if (i < count && amplitudes[i] != 0.0D) this.octaves[i] = new ImprovedNoise(random);
            else random.skip(262);
        }
        if (zeroIndex < count - 1) {
            SeedRandom finer = new SeedRandom((long) (first.noise(0.0D, 0.0D, 0.0D, 0.0D, 0.0D) * 9.223372036854776E18D));
            for (int i = zeroIndex + 1; i < count; i++) {
                if (i >= 0 && amplitudes[i] != 0.0D) this.octaves[i] = new ImprovedNoise(finer);
                else finer.skip(262);
            }
        }
        this.lowestFrequency = Math.pow(2.0D, -zeroIndex);
        this.highestAmplitude = Math.pow(2.0D, count - 1) / (Math.pow(2.0D, count) - 1.0D);
    }

    public double noise(double x, double y, double z) {
        return this.noise(x, y, z, 0.0D, 0.0D, false);
    }

    /** With {@code fixedY}, every octave samples at its own y offset (a 2D slice), as 1.16's depth noise does. */
    public double noise(double x, double y, double z, double yScale, double yMax, boolean fixedY) {
        double total = 0.0D;
        double frequency = this.lowestFrequency;
        double amplitude = this.highestAmplitude;
        for (int i = 0; i < this.octaves.length; i++) {
            ImprovedNoise octave = this.octaves[i];
            if (octave != null) {
                total += this.amplitudes[i] * octave.noise(wrap(x * frequency), fixedY ? -octave.yOffset : wrap(y * frequency), wrap(z * frequency),
                        yScale * frequency, yMax * frequency) * amplitude;
            }
            frequency *= 2.0D;
            amplitude /= 2.0D;
        }
        return total;
    }

    /** 1.16's surface depth noise: a 2D sample whose "y" is the second coordinate. */
    public double surfaceNoise(double x, double z, double yScale, double yMax) {
        return this.noise(x, z, 0.0D, yScale, yMax, false);
    }

    /** The octave {@code index} steps coarser than the finest, or null if it's skipped. */
    @Nullable
    public ImprovedNoise octave(int index) {
        return this.octaves[this.octaves.length - 1 - index];
    }

    /** Keeps huge coordinates from losing precision: wraps them every 2^25. */
    public static double wrap(double value) {
        return value - (double) lfloor(value / 3.3554432E7D + 0.5D) * 3.3554432E7D;
    }

    private static long lfloor(double value) {
        long l = (long) value;
        return value < (double) l ? l - 1L : l;
    }
}
