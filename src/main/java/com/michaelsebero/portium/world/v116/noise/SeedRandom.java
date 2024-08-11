package com.michaelsebero.portium.world.v116.noise;

import java.util.Random;

/** 1.16's seeding of world generation randoms (SharedSeedRandom): per chunk, per feature, and skipping ahead. */
public class SeedRandom extends Random {

    public SeedRandom() {
    }

    public SeedRandom(long seed) {
        super(seed);
    }

    /** Draws and discards {@code bits} single bits, as 1.16 does to keep later noise layers where they were. */
    public void skip(int bits) {
        for (int i = 0; i < bits; i++) this.next(1);
    }

    /** Terrain and surface of one chunk. */
    public long setBaseChunkSeed(int chunkX, int chunkZ) {
        long seed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L;
        this.setSeed(seed);
        return seed;
    }

    /** Decoration of the chunk at the given block corner. */
    public long setDecorationSeed(long worldSeed, int blockX, int blockZ) {
        this.setSeed(worldSeed);
        long a = this.nextLong() | 1L;
        long b = this.nextLong() | 1L;
        long seed = (long) blockX * a + (long) blockZ * b ^ worldSeed;
        this.setSeed(seed);
        return seed;
    }

    /** One feature of one decoration stage, from the chunk's decoration seed. */
    public long setFeatureSeed(long decorationSeed, int index, int stage) {
        long seed = decorationSeed + (long) index + (long) (10000 * stage);
        this.setSeed(seed);
        return seed;
    }

    /** Carvers and structures starting in a chunk. */
    public long setLargeFeatureSeed(long worldSeed, int chunkX, int chunkZ) {
        this.setSeed(worldSeed);
        long a = this.nextLong();
        long b = this.nextLong();
        long seed = (long) chunkX * a ^ (long) chunkZ * b ^ worldSeed;
        this.setSeed(seed);
        return seed;
    }

    /** Structure placement in a region of the spacing grid. */
    public long setLargeFeatureSeedWithSalt(long worldSeed, int regionX, int regionZ, int salt) {
        long seed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L + worldSeed + (long) salt;
        this.setSeed(seed);
        return seed;
    }
}
