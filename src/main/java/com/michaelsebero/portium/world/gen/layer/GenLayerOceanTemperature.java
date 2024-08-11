package com.michaelsebero.portium.world.gen.layer;

import com.michaelsebero.portium.world.biome.OceanTemperature;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

import java.util.Random;

/**
 * 1.13's ocean temperature map: smooth Perlin noise decides where warm, lukewarm, cold and frozen water go.
 * Values are {@link OceanTemperature} ordinals. Zoomed six times before being mixed with the land layer.
 */
public class GenLayerOceanTemperature extends GenLayer {

    private ImprovedNoise noise;

    public GenLayerOceanTemperature(long seed) {
        super(seed);
    }

    @Override
    public void initWorldGenSeed(long seed) {
        super.initWorldGenSeed(seed);
        this.noise = new ImprovedNoise(new Random(seed));
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] out = IntCache.getIntCache(areaWidth * areaHeight);
        for (int z = 0; z < areaHeight; z++) {
            for (int x = 0; x < areaWidth; x++) {
                double value = this.noise.sample((areaX + x) / 8.0D, (areaY + z) / 8.0D, 0.0D);
                out[x + z * areaWidth] = OceanTemperature.fromNoise(value).ordinal();
            }
        }
        return out;
    }
}
