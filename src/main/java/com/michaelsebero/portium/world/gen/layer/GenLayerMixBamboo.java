package com.michaelsebero.portium.world.gen.layer;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

/** Jungle and jungle hills where the bamboo mask is set become bamboo jungle and its hills. */
public class GenLayerMixBamboo extends GenLayer {

    private final GenLayer biomes;
    private final GenLayer mask;
    private final int jungle = Biome.getIdForBiome(Biomes.JUNGLE);
    private final int jungleHills = Biome.getIdForBiome(Biomes.JUNGLE_HILLS);
    private final int bamboo;
    private final int bambooHills;

    public GenLayerMixBamboo(long seed, GenLayer biomes, GenLayer mask, Biome bamboo, Biome bambooHills) {
        super(seed);
        this.biomes = biomes;
        this.mask = mask;
        this.bamboo = Biome.getIdForBiome(bamboo);
        this.bambooHills = Biome.getIdForBiome(bambooHills);
    }

    @Override
    public void initWorldGenSeed(long seed) {
        super.initWorldGenSeed(seed);
        this.biomes.initWorldGenSeed(seed);
        this.mask.initWorldGenSeed(seed);
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] base = this.biomes.getInts(areaX, areaY, areaWidth, areaHeight);
        int[] marks = this.mask.getInts(areaX, areaY, areaWidth, areaHeight);
        int[] out = IntCache.getIntCache(areaWidth * areaHeight);
        for (int i = 0; i < out.length; i++) {
            int biome = base[i];
            if (marks[i] == 1 && biome == this.jungle) biome = this.bamboo;
            else if (marks[i] == 1 && biome == this.jungleHills) biome = this.bambooHills;
            out[i] = biome;
        }
        return out;
    }
}
