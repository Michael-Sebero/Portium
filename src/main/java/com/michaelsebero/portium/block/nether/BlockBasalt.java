package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumFacing;

/** Basalt and polished basalt (1.16): stone that lies along an axis, like logs. */
public class BlockBasalt extends BlockRotatedPillar {

    public BlockBasalt() {
        super(Material.ROCK, MapColor.BLACK);
        this.setHardness(1.25F);
        this.setResistance(4.2F * 5.0F / 3.0F);
        this.setSoundType(PortiumSoundTypes.BASALT);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        // Upright by default, as in 1.16: 1.12's pillar leaves that to subclasses, else the first axis (X) is the
        // default, and blocks placed from it (by world generation) lie on their side.
        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }
}
