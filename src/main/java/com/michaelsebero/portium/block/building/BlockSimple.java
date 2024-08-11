package com.michaelsebero.portium.block.building;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/** A plain full block, used for the smooth stone family and similar decorative blocks. */
public class BlockSimple extends Block {

    public BlockSimple(Material material, MapColor mapColor, SoundType sound) {
        super(material, mapColor);
        this.setSoundType(sound);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    /** A stone-like block: pickaxe required, blast resistance 6 like 1.14's smooth stone. */
    public static BlockSimple stone(MapColor mapColor) {
        BlockSimple block = new BlockSimple(Material.ROCK, mapColor, SoundType.STONE);
        block.setHardness(2.0F).setResistance(10.0F);
        block.setHarvestLevel("pickaxe", 0);
        return block;
    }
}
