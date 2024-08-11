package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumFacing;

/** Crimson and warped stems and hyphae, stripped or not (1.16): logs that don't burn. */
public class BlockNetherStem extends BlockRotatedPillar {

    public BlockNetherStem(MapColor mapColor) {
        super(NetherMaterials.NETHER_WOOD, mapColor);
        this.setHardness(2.0F);
        this.setSoundType(PortiumSoundTypes.HYPHAE);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        // Upright by default, as in 1.16: 1.12's pillar leaves that to subclasses, else the first axis (X) is the
        // default, and blocks placed from it (by world generation) lie on their side.
        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }
}
