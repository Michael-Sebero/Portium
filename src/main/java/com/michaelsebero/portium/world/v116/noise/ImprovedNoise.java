package com.michaelsebero.portium.world.v116.noise;

import java.util.Random;

/** One octave of Perlin's improved noise, as 1.16 samples it (ImprovedNoiseGenerator). */
public final class ImprovedNoise {

    private static final int[][] GRADIENTS = {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}, {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}};

    private final byte[] permutation = new byte[256];
    public final double xOffset;
    public final double yOffset;
    public final double zOffset;

    public ImprovedNoise(Random random) {
        this.xOffset = random.nextDouble() * 256.0D;
        this.yOffset = random.nextDouble() * 256.0D;
        this.zOffset = random.nextDouble() * 256.0D;
        for (int i = 0; i < 256; i++) this.permutation[i] = (byte) i;
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i);
            byte swap = this.permutation[i];
            this.permutation[i] = this.permutation[i + j];
            this.permutation[i + j] = swap;
        }
    }

    /**
     * The noise at a point. With a {@code yScale}, y is snapped down to a multiple of it (no higher than {@code
     * yMax}), which 1.16's terrain uses to repeat noise vertically.
     */
    public double noise(double x, double y, double z, double yScale, double yMax) {
        double px = x + this.xOffset;
        double py = y + this.yOffset;
        double pz = z + this.zOffset;
        int ix = floor(px);
        int iy = floor(py);
        int iz = floor(pz);
        double fx = px - ix;
        double fy = py - iy;
        double fz = pz - iz;
        double snapped = 0.0D;
        if (yScale != 0.0D) snapped = floor(Math.min(yMax, fy) / yScale) * yScale;
        return this.sample(ix, iy, iz, fx, fy - snapped, fz, fade(fx), fade(fy), fade(fz));
    }

    private int perm(int i) {
        return this.permutation[i & 255] & 255;
    }

    private static double grad(int hash, double x, double y, double z) {
        int[] g = GRADIENTS[hash & 15];
        return g[0] * x + g[1] * y + g[2] * z;
    }

    private double sample(int x, int y, int z, double fx, double fy, double fz, double u, double v, double w) {
        int a = this.perm(x) + y;
        int aa = this.perm(a) + z;
        int ab = this.perm(a + 1) + z;
        int b = this.perm(x + 1) + y;
        int ba = this.perm(b) + z;
        int bb = this.perm(b + 1) + z;
        double g000 = grad(this.perm(aa), fx, fy, fz);
        double g100 = grad(this.perm(ba), fx - 1.0D, fy, fz);
        double g010 = grad(this.perm(ab), fx, fy - 1.0D, fz);
        double g110 = grad(this.perm(bb), fx - 1.0D, fy - 1.0D, fz);
        double g001 = grad(this.perm(aa + 1), fx, fy, fz - 1.0D);
        double g101 = grad(this.perm(ba + 1), fx - 1.0D, fy, fz - 1.0D);
        double g011 = grad(this.perm(ab + 1), fx, fy - 1.0D, fz - 1.0D);
        double g111 = grad(this.perm(bb + 1), fx - 1.0D, fy - 1.0D, fz - 1.0D);
        return lerp3(u, v, w, g000, g100, g010, g110, g001, g101, g011, g111);
    }

    static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    static double lerp2(double dx, double dy, double v00, double v10, double v01, double v11) {
        return lerp(dy, lerp(dx, v00, v10), lerp(dx, v01, v11));
    }

    public static double lerp3(double dx, double dy, double dz, double v000, double v100, double v010, double v110, double v001, double v101,
                               double v011, double v111) {
        return lerp(dz, lerp2(dx, dy, v000, v100, v010, v110), lerp2(dx, dy, v001, v101, v011, v111));
    }
}
