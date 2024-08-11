package com.michaelsebero.portium.world.gen.carver;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeDictionary;

import java.util.Random;

/**
 * How 1.13's underwater caves and canyons carve: they cut through the ocean floor and fill with water, the bottom
 * layer at y=10 becomes obsidian or (a quarter of the time) magma, and anything below turns to lava. They never carve
 * above sea level.
 */
final class UnderwaterCarving {

    static final int FLOOR_Y = 10;
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();
    private static final IBlockState MAGMA = Blocks.MAGMA.getDefaultState();
    private static final IBlockState OBSIDIAN = Blocks.OBSIDIAN.getDefaultState();

    /** Underwater carvers start only from chunks in ocean biomes. */
    static boolean startsIn(World world, int chunkX, int chunkZ) {
        Biome biome = world.getBiomeProvider().getBiome(new BlockPos(chunkX << 4, 0, chunkZ << 4));
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN);
    }

    /** Ground the carvers can cut, plus the sand, gravel and liquids of the sea floor. */
    static boolean canCarve(IBlockState state, Biome biome) {
        Material material = state.getMaterial();
        if (material == Material.AIR || material == Material.WATER || material == Material.LAVA) return true;
        net.minecraft.block.Block block = state.getBlock();
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.HARDENED_CLAY || block == Blocks.STAINED_HARDENED_CLAY
                || block == Blocks.SANDSTONE || block == Blocks.RED_SANDSTONE || block == Blocks.MYCELIUM || block == Blocks.SNOW_LAYER || block == Blocks.SAND
                || block == Blocks.GRAVEL || block == Blocks.OBSIDIAN || block == biome.topBlock.getBlock() || block == biome.fillerBlock.getBlock();
    }

    static void carve(ChunkPrimer primer, int x, int y, int z, int seaLevel, Random random) {
        if (y >= seaLevel) return;
        if (y == FLOOR_Y) primer.setBlockState(x, y, z, random.nextFloat() < 0.25F ? MAGMA : OBSIDIAN);
        else primer.setBlockState(x, y, z, y < FLOOR_Y ? LAVA : WATER);
    }

    private UnderwaterCarving() {}
}
