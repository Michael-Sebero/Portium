package com.michaelsebero.portium.world.biome;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeOcean;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

/**
 * The warm, lukewarm and cold oceans of 1.13, shallow or deep. As in 1.13, a lukewarm ocean's floor is sand where
 * it's deep (shallow floors keep dirt), a warm ocean is sand throughout (with sandstone under it), and a cold ocean
 * keeps gravel.
 */
public class BiomePortiumOcean extends BiomeOcean {

    private final OceanTemperature temperature;
    private final boolean deep;

    public BiomePortiumOcean(BiomeProperties properties, OceanTemperature temperature, boolean deep) {
        super(properties);
        this.temperature = temperature;
        this.deep = deep;
        if (temperature == OceanTemperature.WARM) {
            this.topBlock = Blocks.SAND.getDefaultState();
            this.fillerBlock = Blocks.SAND.getDefaultState();
        }
    }

    public OceanTemperature getOceanTemperature() {
        return this.temperature;
    }

    public boolean isDeep() {
        return this.deep;
    }

    @Override
    public void genTerrainBlocks(World world, Random rand, ChunkPrimer primer, int x, int z, double noise) {
        super.genTerrainBlocks(world, rand, primer, x, z, noise);
        if (this.temperature == OceanTemperature.WARM || this.temperature == OceanTemperature.LUKEWARM) {
            // 1.12 hands genTerrainBlocks a column's x and z swapped: it built primer column (z & 15, x & 15).
            replaceFloor(primer, z & 15, x & 15, world.getSeaLevel(), Blocks.SAND.getDefaultState());
        }
    }

    /**
     * Swaps the gravel at the top of a submerged column for {@code floor}: 1.12 always floors deep water with gravel,
     * where 1.13's biomes choose. Water reaches up to one below sea level.
     */
    static void replaceFloor(ChunkPrimer primer, int x, int z, int seaLevel, IBlockState floor) {
        int y = seaLevel - 1;
        while (y > 0 && primer.getBlockState(x, y, z).getMaterial() == Material.WATER) y--;
        for (int i = 0; i < 4 && y > 0 && primer.getBlockState(x, y, z).getBlock() == Blocks.GRAVEL; i++, y--) {
            primer.setBlockState(x, y, z, floor);
        }
    }
}
