package com.michaelsebero.portium.world.gen.feature;

import com.michaelsebero.portium.block.BlockTallSeagrass;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/** A patch of seagrass scattered within 8 blocks of {@code pos} (1.13 SeagrassFeature). */
public class WorldGenSeagrassPatch extends WorldGenerator {

    private final int count;
    private final double tallChance;

    public WorldGenSeagrassPatch(int count, double tallChance) {
        this.count = count;
        this.tallChance = tallChance;
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        boolean placed = false;
        for (int i = 0; i < this.count; i++) {
            int dx = rand.nextInt(8) - rand.nextInt(8);
            int dz = rand.nextInt(8) - rand.nextInt(8);
            BlockPos floor = OceanFloor.at(world, pos.getX() + dx, pos.getZ() + dz);
            if (!WaterUtil.isPlainWaterSource(world.getBlockState(floor))) continue;
            if (rand.nextDouble() < this.tallChance) {
                if (PortiumBlocks.TALL_SEAGRASS.canPlaceBlockAt(world, floor)) {
                    IBlockState lower = PortiumBlocks.TALL_SEAGRASS.getDefaultState();
                    world.setBlockState(floor, lower, 2);
                    world.setBlockState(floor.up(), lower.withProperty(BlockTallSeagrass.HALF, BlockDoublePlant.EnumBlockHalf.UPPER), 2);
                    placed = true;
                }
            } else if (PortiumBlocks.SEAGRASS.canPlaceBlockAt(world, floor)) {
                world.setBlockState(floor, PortiumBlocks.SEAGRASS.getDefaultState(), 2);
                placed = true;
            }
        }
        return placed;
    }
}
