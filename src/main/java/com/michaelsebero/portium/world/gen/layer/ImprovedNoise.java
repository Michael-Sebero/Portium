package com.michaelsebero.portium.world.gen.layer;

import net.minecraft.util.math.MathHelper;

import java.util.Random;

/** Ken Perlin's improved noise, as 1.13 uses for ocean temperatures. Returns roughly -1 to 1. */
public final class ImprovedNoise {

    private static final int[][] GRADIENTS = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}, {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}};

    private final byte[] permutations = new byte[256];
    private final double originX;
    private final double originY;
    private final double originZ;

    public ImprovedNoise(Random random) {
        this.originX = random.nextDouble() * 256.0D;
        this.originY = random.nextDouble() * 256.0D;
        this.originZ = random.nextDouble() * 256.0D;
        for (int i = 0; i < 256; i++) this.permutations[i] = (byte) i;
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i);
            byte swap = this.permutations[i];
            this.permutations[i] = this.permutations[i + j];
            this.permutations[i + j] = swap;
        }
    }

    public double sample(double x, double y, double z) {
        double px = x + this.originX;
        double py = y + this.originY;
        double pz = z + this.originZ;
        int cx = MathHelper.floor(px);
        int cy = MathHelper.floor(py);
        int cz = MathHelper.floor(pz);
        double fx = px - cx;
        double fy = py - cy;
        double fz = pz - cz;
        double u = fade(fx);
        double v = fade(fy);
        double w = fade(fz);

        int a = this.perm(cx) + cy;
        int aa = this.perm(a) + cz;
        int ab = this.perm(a + 1) + cz;
        int b = this.perm(cx + 1) + cy;
        int ba = this.perm(b) + cz;
        int bb = this.perm(b + 1) + cz;

        double x0 = lerp(u, grad(this.perm(aa), fx, fy, fz), grad(this.perm(ba), fx - 1.0D, fy, fz));
        double x1 = lerp(u, grad(this.perm(ab), fx, fy - 1.0D, fz), grad(this.perm(bb), fx - 1.0D, fy - 1.0D, fz));
        double x2 = lerp(u, grad(this.perm(aa + 1), fx, fy, fz - 1.0D), grad(this.perm(ba + 1), fx - 1.0D, fy, fz - 1.0D));
        double x3 = lerp(u, grad(this.perm(ab + 1), fx, fy - 1.0D, fz - 1.0D), grad(this.perm(bb + 1), fx - 1.0D, fy - 1.0D, fz - 1.0D));
        return lerp(w, lerp(v, x0, x1), lerp(v, x2, x3));
    }

    private int perm(int i) {
        return this.permutations[i & 255] & 255;
    }

    private static double grad(int hash, double x, double y, double z) {
        int[] g = GRADIENTS[hash & 15];
        return g[0] * x + g[1] * y + g[2] * z;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
}
