package com.michaelsebero.portium.world.end;

import com.michaelsebero.portium.init.PortiumBiomes;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.13's End biomes: the main island is The End; beyond it, by the outer islands' height (the same island noise 1.12's
 * End generator shapes them with), end highlands, end midlands, end barrens or small end islands. Biomes change by
 * chunk, as in 1.13.
 */
public class BiomeProviderEnd113 extends BiomeProvider {

    private final NoiseGeneratorSimplex islandNoise;
    private final Map<Long, Biome> chunks = new LinkedHashMap<Long, Biome>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Biome> eldest) {
            return this.size() > 4096;
        }
    };

    public BiomeProviderEnd113(long seed) {
        // Drawn from the seed as 1.12's ChunkGeneratorEnd draws its island noise: after its five octave generators.
        Random random = new Random(seed);
        new NoiseGeneratorOctaves(random, 16);
        new NoiseGeneratorOctaves(random, 16);
        new NoiseGeneratorOctaves(random, 8);
        new NoiseGeneratorOctaves(random, 10);
        new NoiseGeneratorOctaves(random, 16);
        this.islandNoise = new NoiseGeneratorSimplex(random);
    }

    /** The biome of a chunk (1.13's {@code TheEndBiomeProvider.getBiome}). */
    public synchronized Biome chunkBiome(int chunkX, int chunkZ) {
        long key = (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
        Biome biome = this.chunks.get(key);
        if (biome == null) {
            biome = this.compute(chunkX, chunkZ);
            this.chunks.put(key, biome);
        }
        return biome;
    }

    private Biome compute(int chunkX, int chunkZ) {
        if ((long) chunkX * chunkX + (long) chunkZ * chunkZ <= 4096L) return Biomes.SKY;
        float height = this.heightValue(chunkX * 2 + 1, chunkZ * 2 + 1);
        if (height > 40.0F) return PortiumBiomes.END_HIGHLANDS;
        if (height >= 0.0F) return PortiumBiomes.END_MIDLANDS;
        return height < -20.0F ? PortiumBiomes.SMALL_END_ISLANDS : PortiumBiomes.END_BARRENS;
    }

    /** The outer islands' height at half-chunk {@code (x, z)}, as 1.12's {@code getIslandHeightValue} works it out. */
    private float heightValue(int x, int z) {
        int chunkX = x / 2;
        int chunkZ = z / 2;
        int offsetX = x % 2;
        int offsetZ = z % 2;
        float height = MathHelper.clamp(100.0F - MathHelper.sqrt((float) (x * x + z * z)) * 8.0F, -100.0F, 80.0F);
        for (int i = -12; i <= 12; ++i) {
            for (int j = -12; j <= 12; ++j) {
                long islandX = chunkX + i;
                long islandZ = chunkZ + j;
                if (islandX * islandX + islandZ * islandZ > 4096L && this.islandNoise.getValue((double) islandX, (double) islandZ) < -0.8999999761581421D) {
                    float size = (MathHelper.abs((float) islandX) * 3439.0F + MathHelper.abs((float) islandZ) * 147.0F) % 13.0F + 9.0F;
                    float dx = (float) (offsetX - i * 2);
                    float dz = (float) (offsetZ - j * 2);
                    float island = MathHelper.clamp(100.0F - MathHelper.sqrt(dx * dx + dz * dz) * size, -100.0F, 80.0F);
                    height = Math.max(height, island);
                }
            }
        }
        return height;
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.chunkBiome(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        return this.getBiome(pos);
    }

    @Override
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
        // Generation's grid is a quarter of a block's resolution.
        if (biomes == null || biomes.length < width * height) biomes = new Biome[width * height];
        for (int dz = 0; dz < height; dz++) {
            for (int dx = 0; dx < width; dx++) biomes[dx + dz * width] = this.chunkBiome((x + dx) >> 2, (z + dz) >> 2);
        }
        return biomes;
    }

    @Override
    public Biome[] getBiomes(@Nullable Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
        if (listToReuse == null || listToReuse.length < width * length) listToReuse = new Biome[width * length];
        for (int dz = 0; dz < length; dz++) {
            for (int dx = 0; dx < width; dx++) listToReuse[dx + dz * width] = this.chunkBiome((x + dx) >> 4, (z + dz) >> 4);
        }
        return listToReuse;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        for (int chunkZ = (z - radius) >> 4; chunkZ <= (z + radius) >> 4; chunkZ++) {
            for (int chunkX = (x - radius) >> 4; chunkX <= (x + radius) >> 4; chunkX++) {
                if (!allowed.contains(this.chunkBiome(chunkX, chunkZ))) return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
        BlockPos found = null;
        int matches = 0;
        for (int chunkZ = (z - range) >> 4; chunkZ <= (z + range) >> 4; chunkZ++) {
            for (int chunkX = (x - range) >> 4; chunkX <= (x + range) >> 4; chunkX++) {
                if (biomes.contains(this.chunkBiome(chunkX, chunkZ)) && (found == null || random.nextInt(matches + 1) == 0)) {
                    found = new BlockPos((chunkX << 4) + 8, 0, (chunkZ << 4) + 8);
                    matches++;
                }
            }
        }
        return found;
    }

    @Override
    public synchronized void cleanupCache() {
        super.cleanupCache();
    }
}
