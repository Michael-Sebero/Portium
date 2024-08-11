package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;

/** Exposes the protected vanilla stairs constructor. */
public class BlockPortiumStairs extends BlockStairs {

    public BlockPortiumStairs(IBlockState modelState) {
        super(modelState);
        this.useNeighborBrightness = true;
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }
}
