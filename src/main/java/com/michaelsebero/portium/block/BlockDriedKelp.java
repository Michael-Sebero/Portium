package com.michaelsebero.portium.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Dried kelp block (1.13): flammable, and burns in a furnace for 4001 ticks. */
public class BlockDriedKelp extends Block {

    public BlockDriedKelp() {
        super(Material.GRASS, MapColor.GREEN);
        this.setHardness(0.5F).setResistance(4.1667F);
        this.setSoundType(SoundType.PLANT);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 30;
    }
}
