package com.michaelsebero.portium.world.buffet;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorEnd;

/**
 * 1.13's Buffet "Floating Islands" generator: the End's islands, unchanged in shape and centred on the origin, made of
 * stone and surfaced and decorated by the chosen biome, over the void.
 */
final class ChunkGeneratorFloatingIslands extends BuffetChunkGenerator {

    private final ChunkGeneratorEnd shape;

    ChunkGeneratorFloatingIslands(World world, BuffetSettings settings) {
        super(world, settings);
        this.shape = new ChunkGeneratorEnd(world, false, world.getSeed(), new BlockPos(0, 64, 0));
    }

    @Override
    protected void shapeTerrain(int chunkX, int chunkZ, ChunkPrimer primer) {
        this.shape.setBlocksInChunk(chunkX, chunkZ, primer);
        IBlockState endStone = Blocks.END_STONE.getDefaultState();
        IBlockState stone = Blocks.STONE.getDefaultState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 128; y++) {
                    if (primer.getBlockState(x, y, z) == endStone) primer.setBlockState(x, y, z, stone);
                }
            }
        }
    }

    @Override
    protected int surfaceSeaLevel() {
        return 0;
    }

    /** 1.12's biome surfaces lay bedrock at the bottom of the world; the islands float over nothing. */
    @Override
    protected void finishTerrain(ChunkPrimer primer) {
        IBlockState air = Blocks.AIR.getDefaultState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 5; y++) {
                    if (primer.getBlockState(x, y, z).getBlock() == Blocks.BEDROCK) primer.setBlockState(x, y, z, air);
                }
            }
        }
    }
}
