package com.michaelsebero.portium.world.buffet;

import net.minecraft.block.state.IBlockState;

/** The overworld generator as a Buffet world's surface generator, mixed into {@code ChunkGeneratorOverworld}. */
public interface IBuffetSurface {

    /** Builds terrain from this block and fluid instead of stone and water. */
    void portium$setDefaults(IBlockState block, IBlockState fluid);
}
