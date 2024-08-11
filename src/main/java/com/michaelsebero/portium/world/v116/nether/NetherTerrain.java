package com.michaelsebero.portium.world.v116.nether;

import com.michaelsebero.portium.world.v116.noise.ImprovedNoise;
import com.michaelsebero.portium.world.v116.noise.OctaveNoise;
import com.michaelsebero.portium.world.v116.noise.SeedRandom;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkPrimer;

/**
 * The shape of 1.16's Nether (NoiseChunkGenerator with the Nether's settings): 3D noise sampled every 4 blocks
 * across and 8 up, interpolated between, netherrack where it's positive and the lava sea below y 32. The roof and
 * floor are forced solid. Also the noise for how deep each column's surface layer goes.
 */
final class NetherTerrain {

    static final int HEIGHT = 128;
    private static final int CELL_WIDTH = 4;
    private static final int CELL_HEIGHT = 8;
    private static final int CELLS_ACROSS = 16 / CELL_WIDTH;
    private static final int CELLS_UP = HEIGHT / CELL_HEIGHT;

    // The Nether's noise settings.
    private static final double XZ_SCALE = 684.412D * 1.0D;
    private static final double Y_SCALE = 684.412D * 3.0D;
    private static final double XZ_MAIN_SCALE = XZ_SCALE / 80.0D;
    private static final double Y_MAIN_SCALE = Y_SCALE / 60.0D;
    private static final double TOP_SLIDE_TARGET = 120.0D;
    private static final double TOP_SLIDE_SIZE = 3.0D;
    private static final double TOP_SLIDE_OFFSET = 0.0D;
    private static final double BOTTOM_SLIDE_TARGET = 320.0D;
    private static final double BOTTOM_SLIDE_SIZE = 4.0D;
    private static final double BOTTOM_SLIDE_OFFSET = -1.0D;
    private static final double DENSITY_FACTOR = 0.0D;
    private static final double DENSITY_OFFSET = 0.019921875D;

    /**
     * What 1.16's biome depth and scale add to the density. Every Nether biome has depth 0.1 and scale 0.2, so the
     * 5x5 average is the same everywhere; worked out once, the float arithmetic exactly as 1.16 does it.
     */
    private static final double BIOME_OFFSET;
    private static final double BIOME_FACTOR;

    static {
        float depthSum = 0.0F;
        float scaleSum = 0.0F;
        float weightSum = 0.0F;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                float kernel = 10.0F / MathHelper.sqrt((float) (dx * dx + dz * dz) + 0.2F);
                float weight = 1.0F * kernel / (0.1F + 2.0F);
                scaleSum += 0.2F * weight;
                depthSum += 0.1F * weight;
                weightSum += weight;
            }
        }
        float depth = depthSum / weightSum;
        float scale = scaleSum / weightSum;
        BIOME_OFFSET = (double) (depth * 0.5F - 0.125F) * 0.265625D;
        BIOME_FACTOR = 96.0D / (double) (scale * 0.9F + 0.1F);
    }

    private static final IBlockState NETHERRACK = Blocks.NETHERRACK.getDefaultState();
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();

    private final OctaveNoise minLimit;
    private final OctaveNoise maxLimit;
    private final OctaveNoise main;
    private final OctaveNoise surfaceDepth;

    NetherTerrain(long seed) {
        // Same order of draws as 1.16, so the same seed makes the same noise.
        SeedRandom random = new SeedRandom(seed);
        this.minLimit = OctaveNoise.ofRange(random, -15, 0);
        this.maxLimit = OctaveNoise.ofRange(random, -15, 0);
        this.main = OctaveNoise.ofRange(random, -7, 0);
        this.surfaceDepth = OctaveNoise.ofRange(random, -3, 0);
    }

    /** How deep the surface goes at a column, 1.16's surface depth noise times 15. */
    double surfaceNoise(int x, int z, int localX) {
        return this.surfaceDepth.surfaceNoise((double) x * 0.0625D, (double) z * 0.0625D, 0.0625D, (double) localX * 0.0625D) * 15.0D;
    }

    /** Fills {@code primer} with the chunk's netherrack and lava. */
    void fill(int chunkX, int chunkZ, ChunkPrimer primer) {
        double[][][] columns = new double[2][CELLS_ACROSS + 1][CELLS_UP + 1];
        for (int cz = 0; cz <= CELLS_ACROSS; cz++) this.fillColumn(columns[0][cz], chunkX * CELLS_ACROSS, chunkZ * CELLS_ACROSS + cz);
        for (int cx = 0; cx < CELLS_ACROSS; cx++) {
            for (int cz = 0; cz <= CELLS_ACROSS; cz++) this.fillColumn(columns[1][cz], chunkX * CELLS_ACROSS + cx + 1, chunkZ * CELLS_ACROSS + cz);
            for (int cz = 0; cz < CELLS_ACROSS; cz++) {
                for (int cy = CELLS_UP - 1; cy >= 0; cy--) {
                    double d000 = columns[0][cz][cy];
                    double d001 = columns[0][cz + 1][cy];
                    double d100 = columns[1][cz][cy];
                    double d101 = columns[1][cz + 1][cy];
                    double d010 = columns[0][cz][cy + 1];
                    double d011 = columns[0][cz + 1][cy + 1];
                    double d110 = columns[1][cz][cy + 1];
                    double d111 = columns[1][cz + 1][cy + 1];
                    for (int sy = CELL_HEIGHT - 1; sy >= 0; sy--) {
                        int y = cy * CELL_HEIGHT + sy;
                        double fy = (double) sy / (double) CELL_HEIGHT;
                        double x0z0 = lerp(fy, d000, d010);
                        double x1z0 = lerp(fy, d100, d110);
                        double x0z1 = lerp(fy, d001, d011);
                        double x1z1 = lerp(fy, d101, d111);
                        for (int sx = 0; sx < CELL_WIDTH; sx++) {
                            int localX = cx * CELL_WIDTH + sx;
                            double fx = (double) sx / (double) CELL_WIDTH;
                            double z0 = lerp(fx, x0z0, x1z0);
                            double z1 = lerp(fx, x0z1, x1z1);
                            for (int sz = 0; sz < CELL_WIDTH; sz++) {
                                int localZ = cz * CELL_WIDTH + sz;
                                double fz = (double) sz / (double) CELL_WIDTH;
                                // 1.16 squashes the density before testing it, keeping its sign; only structures shift it.
                                double density = lerp(fz, z0, z1);
                                if (density > 0.0D) primer.setBlockState(localX, y, localZ, NETHERRACK);
                                else if (y < NetherFeatures.SEA_LEVEL) primer.setBlockState(localX, y, localZ, LAVA);
                            }
                        }
                    }
                }
            }
            double[][] swap = columns[0];
            columns[0] = columns[1];
            columns[1] = swap;
        }
    }

    /** The density at each cell corner up one column of cells. */
    private void fillColumn(double[] column, int cellX, int cellZ) {
        for (int cy = 0; cy <= CELLS_UP; cy++) {
            double density = this.sample(cellX, cy, cellZ);
            double gradient = 1.0D - (double) cy * 2.0D / (double) CELLS_UP;
            double shift = (gradient * DENSITY_FACTOR + DENSITY_OFFSET + BIOME_OFFSET) * BIOME_FACTOR;
            density += shift > 0.0D ? shift * 4.0D : shift;
            density = clampedLerp(TOP_SLIDE_TARGET, density, ((double) (CELLS_UP - cy) - TOP_SLIDE_OFFSET) / TOP_SLIDE_SIZE);
            density = clampedLerp(BOTTOM_SLIDE_TARGET, density, ((double) cy - BOTTOM_SLIDE_OFFSET) / BOTTOM_SLIDE_SIZE);
            column[cy] = density;
        }
    }

    /** 1.16's blended noise: two limit noises, mixed by a third. */
    private double sample(int x, int y, int z) {
        double min = 0.0D;
        double max = 0.0D;
        double main = 0.0D;
        double frequency = 1.0D;
        for (int octave = 0; octave < 16; octave++) {
            double sx = OctaveNoise.wrap((double) x * XZ_SCALE * frequency);
            double sy = OctaveNoise.wrap((double) y * Y_SCALE * frequency);
            double sz = OctaveNoise.wrap((double) z * XZ_SCALE * frequency);
            double yScale = Y_SCALE * frequency;
            ImprovedNoise minOctave = this.minLimit.octave(octave);
            if (minOctave != null) min += minOctave.noise(sx, sy, sz, yScale, (double) y * yScale) / frequency;
            ImprovedNoise maxOctave = this.maxLimit.octave(octave);
            if (maxOctave != null) max += maxOctave.noise(sx, sy, sz, yScale, (double) y * yScale) / frequency;
            if (octave < 8) {
                ImprovedNoise mainOctave = this.main.octave(octave);
                if (mainOctave != null) {
                    main += mainOctave.noise(OctaveNoise.wrap((double) x * XZ_MAIN_SCALE * frequency), OctaveNoise.wrap((double) y * Y_MAIN_SCALE * frequency),
                            OctaveNoise.wrap((double) z * XZ_MAIN_SCALE * frequency), Y_MAIN_SCALE * frequency, (double) y * Y_MAIN_SCALE * frequency) / frequency;
                }
            }
            frequency /= 2.0D;
        }
        return clampedLerp(min / 512.0D, max / 512.0D, (main / 10.0D + 1.0D) / 2.0D);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0D) return start;
        return delta > 1.0D ? end : lerp(delta, start, end);
    }
}
