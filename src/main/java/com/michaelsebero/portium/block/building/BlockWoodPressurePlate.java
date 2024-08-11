package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

/** Exposes the protected vanilla pressure plate constructor for the five new wood types (1.13). */
public class BlockWoodPressurePlate extends BlockPressurePlate {

    private final MapColor mapColor;

    public BlockWoodPressurePlate(MapColor mapColor) {
        super(Material.WOOD, Sensitivity.EVERYTHING);
        this.mapColor = mapColor;
        this.setHardness(0.5F);
        this.setSoundType(SoundType.WOOD);
    }

    @Nonnull
    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.mapColor;
    }
}
