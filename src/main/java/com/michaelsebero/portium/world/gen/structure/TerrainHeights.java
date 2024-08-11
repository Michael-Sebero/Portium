package com.michaelsebero.portium.world.gen.structure;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

/**
 * 1.13's world-generation heightmaps, read only from chunks that are already loaded. Structures span several chunks,
 * and asking for an unloaded chunk while populating would make the game generate it, which can cascade.
 */
final class TerrainHeights {

    /** Highest block that blocks movement, plus one (1.13's OCEAN_FLOOR_WG), or -1 if the chunk isn't loaded. */
    static int oceanFloor(World world, int x, int z) {
        Chunk chunk = loadedChunk(world, x, z);
        if (chunk == null) return -1;
        for (int y = chunk.getTopFilledSegment() + 15; y > 0; y--) {
            if (chunk.getBlockState(x & 15, y, z & 15).getMaterial().blocksMovement()) return y + 1;
        }
        return 0;
    }

    /** Highest non-air block, plus one (1.13's WORLD_SURFACE_WG), or -1 if the chunk isn't loaded. */
    static int surface(World world, int x, int z) {
        Chunk chunk = loadedChunk(world, x, z);
        if (chunk == null) return -1;
        for (int y = chunk.getTopFilledSegment() + 15; y > 0; y--) {
            if (chunk.getBlockState(x & 15, y, z & 15).getMaterial() != Material.AIR) return y + 1;
        }
        return 0;
    }

    @Nullable
    static IBlockState blockState(World world, int x, int y, int z) {
        Chunk chunk = loadedChunk(world, x, z);
        return chunk == null ? null : chunk.getBlockState(x & 15, y, z & 15);
    }

    @Nullable
    private static Chunk loadedChunk(World world, int x, int z) {
        return world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4);
    }

    private TerrainHeights() {}
}
