package com.michaelsebero.portium.block.nether;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/** Crying obsidian (1.16): obsidian that gives light 10, found in ruined portals and bastions. */
public class BlockCryingObsidian extends Block {

    public BlockCryingObsidian() {
        super(Material.ROCK, MapColor.BLACK);
        this.setHardness(50.0F);
        this.setResistance(1200.0F * 5.0F / 3.0F);
        this.setLightLevel(10.0F / 15.0F);
        this.setSoundType(SoundType.STONE);
        this.setHarvestLevel("pickaxe", 3);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }
}
