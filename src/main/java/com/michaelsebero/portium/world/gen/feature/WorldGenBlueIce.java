package com.michaelsebero.portium.world.gen.feature;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/** Clusters of blue ice growing on the underside of packed ice in frozen oceans (1.13 BlueIceFeature). */
public class WorldGenBlueIce extends WorldGenerator {

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        if (pos.getY() > world.getSeaLevel() - 1) return false;
        if (world.getBlockState(pos).getBlock() != Blocks.WATER && world.getBlockState(pos.down()).getBlock() != Blocks.WATER) return false;
        boolean touchesPackedIce = false;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN && world.getBlockState(pos.offset(facing)).getBlock() == Blocks.PACKED_ICE) {
                touchesPackedIce = true;
                break;
            }
        }
        if (!touchesPackedIce) return false;

        IBlockState blueIce = PortiumBlocks.BLUE_ICE.getDefaultState();
        world.setBlockState(pos, blueIce, 2);
        for (int i = 0; i < 200; i++) {
            BlockPos target = pos.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(5), rand.nextInt(8) - rand.nextInt(8));
            IBlockState state = world.getBlockState(target);
            Block block = state.getBlock();
            if (state.getMaterial() == Material.AIR || block == Blocks.WATER || block == Blocks.PACKED_ICE || block == Blocks.ICE) {
                for (EnumFacing facing : EnumFacing.VALUES) {
                    if (world.getBlockState(target.offset(facing)).getBlock() == PortiumBlocks.BLUE_ICE) {
                        world.setBlockState(target, blueIce, 2);
                        break;
                    }
                }
            }
        }
        return true;
    }
}
