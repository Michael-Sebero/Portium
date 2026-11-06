package com.michaelsebero.portium.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

/** A block's item that survives fire and lava (the netherite block and ancient debris, 1.16). */
public class ItemBlockFireproof extends ItemBlock implements IFireproofItem {

    public ItemBlockFireproof(Block block) {
        super(block);
    }
}
