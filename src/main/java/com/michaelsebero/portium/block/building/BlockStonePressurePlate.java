package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

/** A stone pressure plate (mobs and players press it) of a newer stone: 1.16's polished blackstone one. */
public class BlockStonePressurePlate extends BlockPressurePlate {

    private final MapColor mapColor;

    public BlockStonePressurePlate(MapColor mapColor) {
        super(Material.ROCK, Sensitivity.MOBS);
        this.mapColor = mapColor;
        this.setHardness(0.5F);
        this.setSoundType(SoundType.STONE);
    }

    @Nonnull
    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.mapColor;
    }
}
