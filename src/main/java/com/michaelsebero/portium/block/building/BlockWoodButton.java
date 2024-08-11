package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.SoundType;

/** Exposes the protected vanilla wooden button constructor for the five new wood types (1.13). */
public class BlockWoodButton extends BlockButtonWood {

    public BlockWoodButton() {
        this.setHardness(0.5F);
        this.setSoundType(SoundType.WOOD);
    }
}
