package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkCache.class)
public interface ChunkCacheAccessor {

    @Accessor("chunkX")
    int portium$getChunkX();

    @Accessor("chunkZ")
    int portium$getChunkZ();

    @Accessor("chunkArray")
    Chunk[][] portium$getChunks();
}
