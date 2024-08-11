package com.michaelsebero.portium.world.v116.noise;

import com.google.common.hash.Hashing;

/**
 * How 1.16 turns its biomes, kept one per 4x4x4 block cell, into a biome per block (FuzzedBiomeMagnifier): each
 * block takes the biome of whichever nearby cell corner is closest once every corner is nudged by a hash of the
 * world seed, so borders come out ragged rather than in steps of four.
 */
public final class BiomeFuzz {

    /** Looks up the biome of one 4x4x4 cell. */
    public interface CellBiomes<B> {
        B cell(int cellX, int cellY, int cellZ);
    }

    /** The seed 1.16 fuzzes with: the world seed, hashed. */
    public static long hashSeed(long worldSeed) {
        return Hashing.sha256().hashLong(worldSeed).asLong();
    }

    public static <B> B biome(long hashedSeed, int x, int y, int z, CellBiomes<B> cells) {
        int bx = x - 2;
        int by = y - 2;
        int bz = z - 2;
        int cx = bx >> 2;
        int cy = by >> 2;
        int cz = bz >> 2;
        double fx = (double) (bx & 3) / 4.0D;
        double fy = (double) (by & 3) / 4.0D;
        double fz = (double) (bz & 3) / 4.0D;
        int closest = 0;
        double closestDistance = Double.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            boolean lowX = (corner & 4) == 0;
            boolean lowY = (corner & 2) == 0;
            boolean lowZ = (corner & 1) == 0;
            double distance = distanceToCorner(hashedSeed, lowX ? cx : cx + 1, lowY ? cy : cy + 1, lowZ ? cz : cz + 1,
                    lowX ? fx : fx - 1.0D, lowY ? fy : fy - 1.0D, lowZ ? fz : fz - 1.0D);
            // 1.16 keeps the first of equally close corners.
            if (corner == 0 || closestDistance > distance) {
                closest = corner;
                closestDistance = distance;
            }
        }
        return cells.cell((closest & 4) == 0 ? cx : cx + 1, (closest & 2) == 0 ? cy : cy + 1, (closest & 1) == 0 ? cz : cz + 1);
    }

    private static double distanceToCorner(long seed, int x, int y, int z, double dx, double dy, double dz) {
        long hash = mix(seed, x);
        hash = mix(hash, y);
        hash = mix(hash, z);
        hash = mix(hash, x);
        hash = mix(hash, y);
        hash = mix(hash, z);
        double offsetX = offset(hash);
        hash = mix(hash, seed);
        double offsetY = offset(hash);
        hash = mix(hash, seed);
        double offsetZ = offset(hash);
        return square(dz + offsetZ) + square(dy + offsetY) + square(dx + offsetX);
    }

    /** 1.16's FastRandom.mix, a linear congruential step folding in {@code value}. */
    public static long mix(long seed, long value) {
        seed *= seed * 6364136223846793005L + 1442695040888963407L;
        seed += value;
        return seed;
    }

    private static double offset(long hash) {
        double unit = (double) ((int) Math.floorMod(hash >> 24, 1024L)) / 1024.0D;
        return (unit - 0.5D) * 0.9D;
    }

    private static double square(double value) {
        return value * value;
    }

    private BiomeFuzz() {}
}
