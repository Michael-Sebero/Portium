package com.michaelsebero.portium.world.gen.jigsaw;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.16's world-generation heightmap (WORLD_SURFACE_WG: above the highest non-air block) for laying out structures
 * before their chunks exist. Loaded chunks are read as they are; others are worked out from the overworld
 * generator's terrain shape alone ({@code setBlocksInChunk}), which neither loads, decorates nor populates anything,
 * so a layout never sets off chunk generation. Generators other than the overworld's answer sea level.
 */
public final class HeightSampler {

    private final World world;
    /** Recent chunks' heights (16x16 each), as 1.16's noise heightmaps would give them. */
    private final Map<Long, int[]> cache = new LinkedHashMap<Long, int[]>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return this.size() > 256;
        }
    };

    public HeightSampler(World world) {
        this.world = world;
    }

    /**
     * The surface height (the first air above ground or water) at a column, from the terrain's shape as generated, so
     * every layout of a structure comes out the same whatever has loaded or grown since.
     */
    public int surface(int x, int z) {
        return this.column(x, z) & 0xFFFF;
    }

    /** Whether the top block of a column (as generated) is water: a sea, river or other water source. */
    public boolean isWater(int x, int z) {
        return (this.column(x, z) & WATER) != 0;
    }

    private static final int WATER = 1 << 16;

    private int column(int x, int z) {
        long key = (long) (x >> 4) & 0xFFFFFFFFL | ((long) (z >> 4) & 0xFFFFFFFFL) << 32;
        int[] heights = this.cache.get(key);
        if (heights == null) {
            heights = this.generate(x >> 4, z >> 4);
            this.cache.put(key, heights);
        }
        return heights[(z & 15) << 4 | (x & 15)];
    }

    private int[] generate(int chunkX, int chunkZ) {
        int[] heights = new int[256];
        IChunkGenerator generator = this.world instanceof WorldServer ? ((ChunkProviderServer) this.world.getChunkProvider()).chunkGenerator : null;
        if (!(generator instanceof ChunkGeneratorOverworld)) {
            // Other generators: the loaded chunk if there is one, else sea level.
            Chunk loaded = this.world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int height = this.world.getSeaLevel() + 1;
                    if (loaded != null) {
                        for (int y = loaded.getTopFilledSegment() + 15; y > 0; y--) {
                            Material material = loaded.getBlockState(x, y, z).getMaterial();
                            if (material != Material.AIR) {
                                height = y + 1 | (material == Material.WATER ? WATER : 0);
                                break;
                            }
                        }
                    }
                    heights[z << 4 | x] = height;
                }
            }
            return heights;
        }
        ChunkPrimer primer = new ChunkPrimer();
        ((ChunkGeneratorOverworld) generator).setBlocksInChunk(chunkX, chunkZ, primer);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = 0;
                for (int y = 255; y > 0; y--) {
                    IBlockState state = primer.getBlockState(x, y, z);
                    if (state.getMaterial() != Material.AIR) {
                        height = y + 1 | (state.getMaterial() == Material.WATER ? WATER : 0);
                        break;
                    }
                }
                heights[z << 4 | x] = height;
            }
        }
        return heights;
    }
}
