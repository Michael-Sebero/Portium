package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** A fence gate of a newer wood. 1.12's gate takes a vanilla wood type only for its map colour, which this sets itself. */
public class BlockPortiumFenceGate extends BlockFenceGate {

    private final MapColor mapColor;

    public BlockPortiumFenceGate(MapColor mapColor) {
        super(BlockPlanks.EnumType.OAK);
        this.mapColor = mapColor;
        this.setHardness(2.0F);
        this.setResistance(5.0F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.mapColor;
    }
}
