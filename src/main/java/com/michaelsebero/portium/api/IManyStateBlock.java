package com.michaelsebero.portium.api;

import com.michaelsebero.portium.modern.state.ModernStates;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import java.util.List;

/**
 * A block with more states than 1.12's 16 metadata values hold, like many of 1.16's. Its metadata keeps a state's
 * index up to 15; the full state of each cell is kept beside the chunk ({@link ModernStates}), saved with it and sent
 * to the players who see it. Implementers return {@link #metaOf} and {@link #stateOf} from {@code getMetaFromState}
 * and {@code getStateFromMeta}.
 */
public interface IManyStateBlock {

    static int metaOf(IBlockState state) {
        return Math.max(0, Math.min(ModernStates.index(state), 15));
    }

    static IBlockState stateOf(Block block, int meta) {
        List<IBlockState> states = block.getBlockState().getValidStates();
        return states.get(Math.max(0, Math.min(meta, states.size() - 1)));
    }
}
