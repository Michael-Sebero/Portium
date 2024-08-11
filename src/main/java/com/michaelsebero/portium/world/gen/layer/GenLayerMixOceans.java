package com.michaelsebero.portium.world.gen.layer;

import com.michaelsebero.portium.world.biome.OceanTemperature;
import com.michaelsebero.portium.init.PortiumBiomes;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.BiomeDictionary;

/**
 * 1.13's MixOceansLayer: vanilla ocean and deep ocean take the temperature from the ocean temperature layer.
 * Warm and frozen water is softened to lukewarm and cold within 8 cells of land, and deep oceans get the deep
 * variant of their temperature. Other biomes, including modded oceans, pass through untouched.
 */
public class GenLayerMixOceans extends GenLayer {

    private static final int RADIUS = 8;

    private final GenLayer land;
    private final GenLayer temperatures;
    private final boolean[] oceanIds = new boolean[256];
    private final int oceanId;
    private final int deepOceanId;

    public GenLayerMixOceans(long seed, GenLayer land, GenLayer temperatures) {
        super(seed);
        this.land = land;
        this.temperatures = temperatures;
        this.oceanId = Biome.getIdForBiome(Biomes.OCEAN);
        this.deepOceanId = Biome.getIdForBiome(Biomes.DEEP_OCEAN);
        for (Biome biome : Biome.REGISTRY) {
            int id = Biome.getIdForBiome(biome);
            if (id >= 0 && id < 256 && BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) this.oceanIds[id] = true;
        }
    }

    @Override
    public void initWorldGenSeed(long seed) {
        this.land.initWorldGenSeed(seed);
        this.temperatures.initWorldGenSeed(seed);
        super.initWorldGenSeed(seed);
    }

    private boolean isOcean(int id) {
        return id >= 0 && id < 256 && this.oceanIds[id];
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int paddedWidth = areaWidth + RADIUS * 2;
        int[] landInts = this.land.getInts(areaX - RADIUS, areaY - RADIUS, paddedWidth, areaHeight + RADIUS * 2).clone();
        int[] temps = this.temperatures.getInts(areaX, areaY, areaWidth, areaHeight).clone();
        int[] out = IntCache.getIntCache(areaWidth * areaHeight);
        for (int z = 0; z < areaHeight; z++) {
            for (int x = 0; x < areaWidth; x++) {
                int landId = landInts[(x + RADIUS) + (z + RADIUS) * paddedWidth];
                if (landId != this.oceanId && landId != this.deepOceanId) {
                    out[x + z * areaWidth] = landId;
                    continue;
                }
                OceanTemperature temperature = OceanTemperature.values()[temps[x + z * areaWidth]];
                if (temperature == OceanTemperature.WARM || temperature == OceanTemperature.FROZEN) {
                    search:
                    for (int dz = -RADIUS; dz <= RADIUS; dz += 4) {
                        for (int dx = -RADIUS; dx <= RADIUS; dx += 4) {
                            if (!this.isOcean(landInts[(x + RADIUS + dx) + (z + RADIUS + dz) * paddedWidth])) {
                                temperature = temperature == OceanTemperature.WARM ? OceanTemperature.LUKEWARM : OceanTemperature.COLD;
                                break search;
                            }
                        }
                    }
                }
                out[x + z * areaWidth] = PortiumBiomes.oceanId(temperature, landId == this.deepOceanId);
            }
        }
        return out;
    }
}
