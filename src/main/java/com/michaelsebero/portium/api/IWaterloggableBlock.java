package com.michaelsebero.portium.api;

import net.minecraft.block.state.IBlockState;

/**
 * For other mods' blocks: implementing this lets a block hold a water source in its cell, like 1.13's waterloggable
 * blocks. Subclasses of the vanilla blocks 1.13 makes waterloggable (stairs, slabs, fences...) don't need it.
 */
public interface IWaterloggableBlock {

    /** Whether this state can hold water; a double slab, for example, fills its whole cell. */
    default boolean canBeWaterlogged(IBlockState state) {
        return true;
    }
}
