package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockFence;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/** A wooden fence of a newer wood (1.16's crimson and warped); connects to every wooden fence, as 1.16's do. */
public class BlockPortiumFence extends BlockFence {

    public BlockPortiumFence(MapColor mapColor) {
        super(Material.WOOD, mapColor);
        this.setHardness(2.0F);
        this.setResistance(5.0F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }
}
