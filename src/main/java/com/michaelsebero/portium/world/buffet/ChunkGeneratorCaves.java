package com.michaelsebero.portium.world.buffet;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorHell;

/**
 * 1.13's Buffet "Caves" generator: the Nether's terrain, unchanged in shape, made of stone with water below y 32 and
 * bedrock above and below, surfaced and decorated by the chosen biome.
 */
final class ChunkGeneratorCaves extends BuffetChunkGenerator {

    private final ChunkGeneratorHell shape;

    ChunkGeneratorCaves(World world, BuffetSettings settings) {
        super(world, settings);
        this.shape = new ChunkGeneratorHell(world, false, world.getSeed());
    }

    @Override
    protected void shapeTerrain(int chunkX, int chunkZ, ChunkPrimer primer) {
        this.shape.prepareHeights(chunkX, chunkZ, primer);
        IBlockState netherrack = Blocks.NETHERRACK.getDefaultState();
        IBlockState lava = Blocks.LAVA.getDefaultState();
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState water = Blocks.WATER.getDefaultState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 128; y++) {
                    IBlockState state = primer.getBlockState(x, y, z);
                    if (state == netherrack) primer.setBlockState(x, y, z, stone);
                    else if (state == lava) primer.setBlockState(x, y, z, water);
                }
            }
        }
    }

    @Override
    protected int surfaceSeaLevel() {
        return 32;
    }

    /** Five uneven layers of bedrock at the floor and the roof, as the Nether has. */
    @Override
    protected void finishTerrain(ChunkPrimer primer) {
        IBlockState bedrock = Blocks.BEDROCK.getDefaultState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 127; y > 122; y--) {
                    if (y >= 127 - this.rand.nextInt(5)) primer.setBlockState(x, y, z, bedrock);
                }
                for (int y = 4; y >= 0; y--) {
                    if (y <= this.rand.nextInt(5)) primer.setBlockState(x, y, z, bedrock);
                }
            }
        }
    }
}
