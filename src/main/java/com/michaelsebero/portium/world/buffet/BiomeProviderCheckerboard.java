package com.michaelsebero.portium.world.buffet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 1.13's checkerboard biome source: the listed biomes in turn along diagonal bands of squares 2<sup>size + 4</sup>
 * blocks wide. As in 1.13 the coarse grid that shapes terrain uses the same pattern at its own coordinates, a structure
 * fits only if every listed biome allows it, and there is no biome to search for a spawn point.
 */
public class BiomeProviderCheckerboard extends BiomeProvider {

    private final Biome[] biomes;
    private final int shift;

    public BiomeProviderCheckerboard(Biome[] biomes, int size) {
        this.biomes = biomes;
        this.shift = size + 4;
    }

    private Biome at(int x, int z) {
        return this.biomes[Math.abs(((x >> this.shift) + (z >> this.shift)) % this.biomes.length)];
    }

    private Biome[] fill(@Nullable Biome[] out, int x, int z, int width, int depth) {
        if (out == null || out.length < width * depth) out = new Biome[width * depth];
        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) out[dz * width + dx] = this.at(x + dx, z + dz);
        }
        return out;
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.at(pos.getX(), pos.getZ());
    }

    @Override
    public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        return this.at(pos.getX(), pos.getZ());
    }

    @Override
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
        return this.fill(biomes, x, z, width, height);
    }

    @Override
    public Biome[] getBiomes(@Nullable Biome[] biomes, int x, int z, int width, int depth) {
        return this.fill(biomes, x, z, width, depth);
    }

    @Override
    public Biome[] getBiomes(@Nullable Biome[] biomes, int x, int z, int width, int length, boolean cacheFlag) {
        return this.fill(biomes, x, z, width, length);
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        return allowed.containsAll(Arrays.asList(this.biomes));
    }

    @Nullable
    @Override
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
        return null;
    }
}
