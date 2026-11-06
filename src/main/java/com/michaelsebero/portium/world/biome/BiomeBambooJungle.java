package com.michaelsebero.portium.world.biome;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.gen.plant.Plants114;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeJungle;

import java.util.Random;

/**
 * 1.14's bamboo jungle (and its hills): a jungle thick with bamboo standing on patches of podzol, with fewer trees.
 * Replaces one jungle region in ten in worlds made with 1.16.5's generation.
 */
public class BiomeBambooJungle extends BiomeJungle {

    public BiomeBambooJungle(BiomeProperties properties) {
        super(false, properties);
        this.decorator.treesPerChunk = 30;
        this.decorator.grassPerChunk = 25;
    }

    @Override
    public void decorate(World world, Random random, BlockPos pos) {
        // Bamboo first, then the jungle's trees, as 1.14 orders its bamboo jungle's features.
        this.bamboo(world, random, pos);
        super.decorate(world, random, pos);
    }

    private void bamboo(World world, Random random, BlockPos pos) {
        if (PortiumBlocks.BAMBOO == null) return;
        // As 1.14's dense bamboo: many stalks per chunk, with podzol worked into the ground around some of them.
        for (int i = 0; i < 160; i++) {
            int x = random.nextInt(16) + 8;
            int z = random.nextInt(16) + 8;
            BlockPos top = world.getHeight(pos.add(x, 0, z));
            if (random.nextFloat() < 0.2F) podzol(world, random, top.down());
            Plants114.bamboo(world, random, top);
        }
    }

    private static void podzol(World world, Random random, BlockPos center) {
        int radius = 1 + random.nextInt(3);
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-radius, -1, -radius), center.add(radius, 1, radius))) {
            int dx = pos.getX() - center.getX(), dz = pos.getZ() - center.getZ();
            if (dx * dx + dz * dz > radius * radius) continue;
            if (world.getBlockState(pos).getBlock() == Blocks.GRASS) {
                world.setBlockState(pos, Blocks.DIRT.getStateFromMeta(2), 2);
            }
        }
    }
}
