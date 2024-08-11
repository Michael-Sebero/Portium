package com.michaelsebero.portium.block.coral;

import net.minecraft.block.Block;

/**
 * Live coral turns into its dead counterpart when it dries out.
 */
public interface ICoral {

    CoralType getCoralType();

    boolean isDead();

    /** The dead variant of this block, or {@code null} if this block is already dead. */
    Block getDeadBlock();

    /** Delay before live coral re-checks for water, 60-99 ticks as in 1.13. */
    static int dryingDelay(java.util.Random rand) {
        return 60 + rand.nextInt(40);
    }
}
