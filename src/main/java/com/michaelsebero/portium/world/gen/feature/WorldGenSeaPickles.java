package com.michaelsebero.portium.world.gen.feature;

import com.michaelsebero.portium.block.BlockSeaPickle;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/** Sea pickles scattered within 8 blocks of {@code pos} (1.13 SeaPickleFeature). */
public class WorldGenSeaPickles extends WorldGenerator {

    private final int count;

    public WorldGenSeaPickles(int count) {
        this.count = count;
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        boolean placed = false;
        for (int i = 0; i < this.count; i++) {
            BlockPos floor = OceanFloor.at(world, pos.getX() + rand.nextInt(8) - rand.nextInt(8), pos.getZ() + rand.nextInt(8) - rand.nextInt(8));
            if (WaterUtil.isPlainWaterSource(world.getBlockState(floor)) && PortiumBlocks.SEA_PICKLE.canPlaceBlockAt(world, floor)) {
                world.setBlockState(floor, PortiumBlocks.SEA_PICKLE.getDefaultState().withProperty(BlockSeaPickle.PICKLES, rand.nextInt(4) + 1), 2);
                placed = true;
            }
        }
        return placed;
    }
}
