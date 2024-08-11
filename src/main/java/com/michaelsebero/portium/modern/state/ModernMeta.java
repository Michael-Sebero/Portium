package com.michaelsebero.portium.modern.state;

import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import net.minecraft.block.state.IBlockState;

/** The metadata 1.12 stores for a 1.13 block's state (see {@link ModernBlocks#metaFromState}). */
final class ModernMeta {

    static int meta(IBlockState state) {
        return Math.max(0, Math.min(ModernStates.index(state), 15));
    }

    private ModernMeta() {}
}
