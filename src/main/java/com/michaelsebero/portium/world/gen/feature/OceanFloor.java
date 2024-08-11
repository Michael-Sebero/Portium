package com.michaelsebero.portium.world.gen.feature;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** 1.13's OCEAN_FLOOR heightmap: the first position above the highest block that blocks movement. */
public final class OceanFloor {

    public static BlockPos at(World world, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, world.getHeight(x, z), z);
        while (pos.getY() > 0) {
            pos.move(net.minecraft.util.EnumFacing.DOWN);
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial().blocksMovement() && !(state.getBlock() instanceof BlockLiquid)) {
                return pos.up();
            }
        }
        return new BlockPos(x, 0, z);
    }

    private OceanFloor() {}
}
