package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockButtonStone;
import net.minecraft.block.SoundType;

/** A stone button of a newer stone (1.16's polished blackstone button); 1.12's constructor is protected. */
public class BlockStoneButton extends BlockButtonStone {

    public BlockStoneButton() {
        this.setHardness(0.5F);
        this.setSoundType(SoundType.STONE);
    }
}
