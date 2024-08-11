package com.michaelsebero.portium.world.v116.nether;

import com.michaelsebero.portium.init.PortiumBiomes;
import com.michaelsebero.portium.world.v116.noise.BiomeFuzz;
import com.michaelsebero.portium.world.v116.noise.NormalNoise;
import com.michaelsebero.portium.world.v116.noise.SeedRandom;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.BiomeProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.16's Nether biome source (its "nether" preset): four noises (temperature, humidity, altitude, weirdness) pick
 * whichever biome's point is nearest, per 4x4 cell, and the cells are fuzzed into a biome per block as 1.16 does.
 * 1.12 keeps one biome per column, so a column takes the biome 1.16 gives it at the top of the Nether (y 128), where
 * 1.16 looks up the biome that builds the column's surface.
 */
public class BiomeProviderNether116 extends BiomeProvider {

    /** The height whose biome a column takes. */
    public static final int COLUMN_Y = 128;

    private static final double[] AMPLITUDES = {1.0D, 1.0D};
    private static final int FIRST_OCTAVE = -7;

    private final NormalNoise temperature;
    private final NormalNoise humidity;
    private final NormalNoise altitude;
    private final NormalNoise weirdness;
    private final List<Point> points = new ArrayList<>();
    private final long hashedSeed;
    /** Column biomes a chunk at a time, as 1.12 keeps them (its own cache is out of reach). */
    private final BiomeCache columns = new BiomeCache(this);

    public BiomeProviderNether116(long seed) {
        this.temperature = new NormalNoise(new SeedRandom(seed), FIRST_OCTAVE, AMPLITUDES);
        this.humidity = new NormalNoise(new SeedRandom(seed + 1L), FIRST_OCTAVE, AMPLITUDES);
        this.altitude = new NormalNoise(new SeedRandom(seed + 2L), FIRST_OCTAVE, AMPLITUDES);
        this.weirdness = new NormalNoise(new SeedRandom(seed + 3L), FIRST_OCTAVE, AMPLITUDES);
        this.hashedSeed = BiomeFuzz.hashSeed(seed);
        // 1.16's points, in its order (it keeps the first of equally near ones). A biome switched off leaves the others.
        this.addPoint(Biomes.HELL, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        this.addPoint(PortiumBiomes.SOUL_SAND_VALLEY, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F);
        this.addPoint(PortiumBiomes.CRIMSON_FOREST, 0.4F, 0.0F, 0.0F, 0.0F, 0.0F);
        this.addPoint(PortiumBiomes.WARPED_FOREST, 0.0F, 0.5F, 0.0F, 0.0F, 0.375F);
        this.addPoint(PortiumBiomes.BASALT_DELTAS, -0.5F, 0.0F, 0.0F, 0.0F, 0.175F);
    }

    private void addPoint(@Nullable Biome biome, float temperature, float humidity, float altitude, float weirdness, float offset) {
        if (biome != null) this.points.add(new Point(biome, temperature, humidity, altitude, weirdness, offset));
    }

    /** The biome of the 4x4 cell column at {@code (cellX, cellZ)}: the Nether's ignore height. */
    public Biome cellBiome(int cellX, int cellZ) {
        float t = (float) this.temperature.noise(cellX, 0.0D, cellZ);
        float h = (float) this.humidity.noise(cellX, 0.0D, cellZ);
        float a = (float) this.altitude.noise(cellX, 0.0D, cellZ);
        float w = (float) this.weirdness.noise(cellX, 0.0D, cellZ);
        Point nearest = null;
        float nearestDistance = 0.0F;
        for (Point point : this.points) {
            float distance = point.distance(t, h, a, w);
            if (nearest == null || Float.compare(distance, nearestDistance) < 0) {
                nearest = point;
                nearestDistance = distance;
            }
        }
        return nearest == null ? Biomes.HELL : nearest.biome;
    }

    /** The biome 1.16 gives the block at {@code (x, y, z)}. */
    public Biome biomeAt(int x, int y, int z) {
        return BiomeFuzz.biome(this.hashedSeed, x, y, z, (cellX, cellY, cellZ) -> this.cellBiome(cellX, cellZ));
    }

    @Override
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
        if (biomes == null || biomes.length < width * height) biomes = new Biome[width * height];
        for (int dz = 0; dz < height; dz++) {
            for (int dx = 0; dx < width; dx++) biomes[dx + dz * width] = this.cellBiome(x + dx, z + dz);
        }
        return biomes;
    }

    @Override
    public Biome[] getBiomes(@Nullable Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
        if (listToReuse == null || listToReuse.length < width * length) listToReuse = new Biome[width * length];
        if (cacheFlag && width == 16 && length == 16 && (x & 15) == 0 && (z & 15) == 0) {
            System.arraycopy(this.columns.getCachedBiomes(x, z), 0, listToReuse, 0, width * length);
            return listToReuse;
        }
        // The cells the fuzz can pick from: one either side of the ones the blocks fall in.
        int fromX = (x - 2) >> 2;
        int fromZ = (z - 2) >> 2;
        int cellsX = ((x + width - 1 - 2) >> 2) - fromX + 2;
        int cellsZ = ((z + length - 1 - 2) >> 2) - fromZ + 2;
        Biome[] cells = new Biome[cellsX * cellsZ];
        BiomeFuzz.CellBiomes<Biome> lookup = (cellX, cellY, cellZ) -> {
            int index = (cellX - fromX) + (cellZ - fromZ) * cellsX;
            Biome biome = cells[index];
            if (biome == null) cells[index] = biome = this.cellBiome(cellX, cellZ);
            return biome;
        };
        for (int dz = 0; dz < length; dz++) {
            for (int dx = 0; dx < width; dx++) {
                listToReuse[dx + dz * width] = BiomeFuzz.biome(this.hashedSeed, x + dx, COLUMN_Y, z + dz, lookup);
            }
        }
        return listToReuse;
    }

    @Override
    public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        return this.columns.getBiome(pos.getX(), pos.getZ(), defaultBiome);
    }

    @Override
    public void cleanupCache() {
        this.columns.cleanupCache();
        super.cleanupCache();
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        int fromX = (x - radius) >> 2;
        int fromZ = (z - radius) >> 2;
        int toX = (x + radius) >> 2;
        int toZ = (z + radius) >> 2;
        for (int cellZ = fromZ; cellZ <= toZ; cellZ++) {
            for (int cellX = fromX; cellX <= toX; cellX++) {
                if (!allowed.contains(this.cellBiome(cellX, cellZ))) return false;
            }
        }
        return true;
    }

    @Override
    @Nullable
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
        int fromX = (x - range) >> 2;
        int fromZ = (z - range) >> 2;
        int width = ((x + range) >> 2) - fromX + 1;
        int depth = ((z + range) >> 2) - fromZ + 1;
        BlockPos found = null;
        int matches = 0;
        for (int i = 0; i < width * depth; i++) {
            int cellX = fromX + i % width;
            int cellZ = fromZ + i / width;
            if (biomes.contains(this.cellBiome(cellX, cellZ)) && (found == null || random.nextInt(matches + 1) == 0)) {
                found = new BlockPos(cellX << 2, 0, cellZ << 2);
                matches++;
            }
        }
        return found;
    }

    /** A biome's place in 1.16's climate space. */
    private static final class Point {

        final Biome biome;
        final float temperature;
        final float humidity;
        final float altitude;
        final float weirdness;
        final float offset;

        Point(Biome biome, float temperature, float humidity, float altitude, float weirdness, float offset) {
            this.biome = biome;
            this.temperature = temperature;
            this.humidity = humidity;
            this.altitude = altitude;
            this.weirdness = weirdness;
            this.offset = offset;
        }

        /** 1.16's attribute difference, in float as it computes it; the sample's offset is 0. */
        float distance(float t, float h, float a, float w) {
            return (this.temperature - t) * (this.temperature - t) + (this.humidity - h) * (this.humidity - h)
                    + (this.altitude - a) * (this.altitude - a) + (this.weirdness - w) * (this.weirdness - w)
                    + (this.offset - 0.0F) * (this.offset - 0.0F);
        }
    }
}
