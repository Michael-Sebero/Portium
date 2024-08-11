package com.michaelsebero.portium.world.gen.feature;

import com.michaelsebero.portium.block.BlockKelp;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/** One kelp column (1.13 KelpFeature): 1-11 blocks tall, stopping below the surface. {@code pos} is the ocean floor. */
public class WorldGenKelpColumn extends WorldGenerator {

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        if (!WaterUtil.isPlainWaterSource(world.getBlockState(pos))) return false;
        BlockKelp kelp = (BlockKelp) PortiumBlocks.KELP;
        IBlockState plant = PortiumBlocks.KELP_PLANT.getDefaultState();
        int height = 1 + rand.nextInt(10);
        boolean placed = false;
        BlockPos current = pos;
        for (int i = 0; i <= height; i++) {
            boolean roomAbove = WaterUtil.isPlainWaterSource(world.getBlockState(current.up()));
            if (WaterUtil.isPlainWaterSource(world.getBlockState(current)) && roomAbove && PortiumBlocks.KELP_PLANT.canPlaceBlockAt(world, current)) {
                world.setBlockState(current, i == height ? kelp.getDefaultState().withProperty(BlockKelp.AGE, rand.nextInt(BlockKelp.MAX_AGE - 1)) : plant, 2);
                placed = true;
            } else {
                if (i > 0) {
                    BlockPos top = current.down();
                    if (world.getBlockState(top).getBlock() == PortiumBlocks.KELP_PLANT) {
                        world.setBlockState(top, kelp.getDefaultState().withProperty(BlockKelp.AGE, rand.nextInt(BlockKelp.MAX_AGE - 1)), 2);
                    }
                }
                break;
            }
            current = current.up();
        }
        return placed;
    }
}
