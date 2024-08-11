package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

/** Exposes the protected vanilla trapdoor constructor for the five new wood types (1.13). */
public class BlockWoodTrapDoor extends BlockTrapDoor {

    private final MapColor mapColor;

    public BlockWoodTrapDoor(MapColor mapColor) {
        super(Material.WOOD);
        this.mapColor = mapColor;
        this.setHardness(3.0F);
        this.setSoundType(SoundType.WOOD);
    }

    @Nonnull
    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.mapColor;
    }
}
