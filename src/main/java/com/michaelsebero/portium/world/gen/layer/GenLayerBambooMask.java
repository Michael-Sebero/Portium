package com.michaelsebero.portium.world.gen.layer;

import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

/** One cell in ten marked (1.14's AddBambooForestLayer roll), at the biome layer's coarse scale, to be zoomed up. */
public class GenLayerBambooMask extends GenLayer {

    public GenLayerBambooMask(long seed) {
        super(seed);
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] out = IntCache.getIntCache(areaWidth * areaHeight);
        for (int z = 0; z < areaHeight; z++) {
            for (int x = 0; x < areaWidth; x++) {
                this.initChunkSeed(areaX + x, areaY + z);
                out[x + z * areaWidth] = this.nextInt(10) == 0 ? 1 : 0;
            }
        }
        return out;
    }
}
