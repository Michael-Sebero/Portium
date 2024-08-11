package com.michaelsebero.portium.modern.host;

import com.michaelsebero.portium.modern.shim.ChunkShims;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;

/** 1.13's {@code Chunk.getTopBlockY}, which names 1.13's height map kinds: Portium looks down the column by name. */
public final class ChunkGlue {

    public static int getTopBlockY(Chunk chunk, Heightmap.Type type, int x, int z) {
        return ChunkShims.getTopBlockY(chunk, type.name(), x, z);
    }

    private ChunkGlue() {}
}
