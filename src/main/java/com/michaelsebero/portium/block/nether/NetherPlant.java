package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/** The ground 1.16's Nether plants grow on. */
final class NetherPlant {

    /** Nylium and soul soil, and the Overworld soils every bush takes (1.16's NetherRootsBlock and NetherSproutsBlock). */
    static boolean isRootGround(IBlockState state) {
        Block block = state.getBlock();
        return BlockNylium.isNylium(block) || block == PortiumBlocks.SOUL_SOIL || isOverworldSoil(block);
    }

    /** As roots, and mycelium too (1.16's FungusBlock). */
    static boolean isFungusGround(IBlockState state) {
        return isRootGround(state) || state.getBlock() == Blocks.MYCELIUM;
    }

    private static boolean isOverworldSoil(Block block) {
        return block == Blocks.GRASS || block == Blocks.DIRT || block == Blocks.FARMLAND;
    }

    private NetherPlant() {}
}
