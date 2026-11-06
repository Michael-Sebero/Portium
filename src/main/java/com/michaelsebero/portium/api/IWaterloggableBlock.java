package com.michaelsebero.portium.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * For other mods' blocks: implementing this lets a block hold a water source in its cell, like 1.13's waterloggable
 * blocks. Subclasses of the vanilla blocks 1.13 makes waterloggable (stairs, slabs, fences...) don't need it.
 */
public interface IWaterloggableBlock {

    /** Whether this state can hold water; a double slab, for example, fills its whole cell. */
    default boolean canBeWaterlogged(IBlockState state) {
        return true;
    }

    /**
     * Called after water filled the block's cell (a bucket, or the block placed in water), on the side that changed
     * it; 1.13's {@code ILiquidContainer.receiveFluid}. A campfire goes out, for example.
     */
    default void onWaterlogged(World world, BlockPos pos, IBlockState state) {}
}
