package com.michaelsebero.portium.world.biome;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeOcean;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import java.util.Random;

/**
 * The 1.13 frozen ocean and deep frozen ocean. The surface is broken by jagged packed-ice formations capped with
 * snow, and on the shallow variant the water only freezes in patches. Replaces vanilla's frozen ocean, which 1.12
 * never generated in normal worlds.
 */
public class BiomeFrozenOcean extends BiomeOcean {

    private static final IBlockState PACKED_ICE = Blocks.PACKED_ICE.getDefaultState();
    private static final IBlockState SNOW_BLOCK = Blocks.SNOW.getDefaultState();
    private static final NoiseGeneratorPerlin FROZEN_PATCH_NOISE = new NoiseGeneratorPerlin(new Random(3456L), 3);

    private final boolean deep;
    private long surfaceSeed;
    private NoiseGeneratorPerlin pillarNoise;
    private NoiseGeneratorPerlin pillarRoofNoise;

    public BiomeFrozenOcean(BiomeProperties properties, boolean deep) {
        super(properties);
        this.deep = deep;
        this.spawnableCreatureList.add(new SpawnListEntry(EntityPolarBear.class, 1, 1, 2));
    }

    public boolean isDeep() {
        return this.deep;
    }

    /**
     * Shallow frozen oceans only freeze over in patches (1.13). {@link Biome#getTemperature} is final in 1.12, so
     * {@code MixinBiome} routes it here.
     */
    public float getPatchyTemperature(BlockPos pos) {
        float base = this.getDefaultTemperature();
        double patch = FROZEN_PATCH_NOISE.getValue(pos.getX() * 0.05D, pos.getZ() * 0.05D) * 7.0D;
        double detail = GRASS_COLOR_NOISE.getValue(pos.getX() * 0.2D, pos.getZ() * 0.2D);
        if (patch + detail < 0.3D && GRASS_COLOR_NOISE.getValue(pos.getX() * 0.09D, pos.getZ() * 0.09D) < 0.8D) base = 0.2F;
        if (pos.getY() > 64) {
            float height = (float) (TEMPERATURE_NOISE.getValue(pos.getX() / 8.0F, pos.getZ() / 8.0F) * 4.0D);
            return base - (height + pos.getY() - 64.0F) * 0.05F / 30.0F;
        }
        return base;
    }

    @Override
    public void genTerrainBlocks(World world, Random rand, ChunkPrimer primer, int x, int z, double noise) {
        if (!PortiumConfig.worldGen.frozenOceanSurface || !WorldGeneration.isModern(world)) {
            super.genTerrainBlocks(world, rand, primer, x, z, noise);
            return;
        }
        this.ensureNoise(world.getSeed());
        int seaLevel = world.getSeaLevel();
        // 1.12 hands genTerrainBlocks a column's x and z swapped within the chunk (it builds primer column
        // (z & 15, x & 15)); 1.13 builds each column at its own position, so the icebergs go where 1.13 puts them.
        int cx = z & 15;
        int cz = x & 15;
        int worldX = (x & ~15) | cx;
        int worldZ = (z & ~15) | cz;
        double iceTop = 0.0D;
        double iceBottom = 0.0D;
        double spike = Math.min(Math.abs(noise), this.pillarNoise.getValue(worldX * 0.1D, worldZ * 0.1D) * 15.0D);
        if (spike > 1.8D) {
            double roof = Math.abs(this.pillarRoofNoise.getValue(worldX * 0.09765625D, worldZ * 0.09765625D));
            iceTop = Math.min(spike * spike * 1.2D, Math.ceil(roof * 40.0D) + 14.0D);
            if (this.getTemperature(new BlockPos(worldX, 63, worldZ)) > 0.1F) iceTop -= 2.0D;
            if (iceTop > 2.0D) {
                iceBottom = seaLevel - iceTop - 7.0D;
                iceTop += seaLevel;
            } else {
                iceTop = 0.0D;
            }
        }

        for (int y = Math.max(seaLevel + 1, (int) iceTop + 1); y > 0; y--) {
            Material material = primer.getBlockState(cx, y, cz).getMaterial();
            if (material == Material.AIR && y < (int) iceTop && rand.nextDouble() > 0.01D) {
                primer.setBlockState(cx, y, cz, PACKED_ICE);
            } else if (material == Material.WATER && y > (int) iceBottom && y < seaLevel && iceBottom != 0.0D && rand.nextDouble() > 0.15D) {
                primer.setBlockState(cx, y, cz, PACKED_ICE);
            }
        }

        super.genTerrainBlocks(world, rand, primer, x, z, noise);

        int maxSnow = 2 + rand.nextInt(4);
        int snowLine = seaLevel + 18 + rand.nextInt(10);
        int snow = 0;
        for (int y = Math.max(seaLevel + 1, (int) iceTop + 1); y > snowLine && snow <= maxSnow; y--) {
            if (primer.getBlockState(cx, y, cz) == PACKED_ICE) {
                primer.setBlockState(cx, y, cz, SNOW_BLOCK);
                snow++;
            }
        }
    }

    private void ensureNoise(long seed) {
        if (this.pillarNoise == null || this.surfaceSeed != seed) {
            Random random = new Random(seed);
            this.pillarNoise = new NoiseGeneratorPerlin(random, 4);
            this.pillarRoofNoise = new NoiseGeneratorPerlin(random, 1);
            this.surfaceSeed = seed;
        }
    }
}
