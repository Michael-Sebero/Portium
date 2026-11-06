package com.michaelsebero.portium.block.bee;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/** 1.15's rule for what pistons pull together: anything next to a sticky block, except honey and slime with each other. */
public final class StickyBlocks {

    public static boolean canStickTo(Block a, Block b) {
        if (a instanceof BlockHoney && b == Blocks.SLIME_BLOCK || a == Blocks.SLIME_BLOCK && b instanceof BlockHoney) return false;
        return a.isStickyBlock(a.getDefaultState()) || b.isStickyBlock(b.getDefaultState());
    }

    private StickyBlocks() {}
}
