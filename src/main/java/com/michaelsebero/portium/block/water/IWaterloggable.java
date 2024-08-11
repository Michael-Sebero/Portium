package com.michaelsebero.portium.block.water;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;

/**
 * A Portium block that can hold a water source in its own cell, standing in for 1.13 waterlogging.
 * <p>
 * While waterlogged the block reports {@link net.minecraft.block.material.Material#WATER}, so vanilla swimming,
 * drowning, fog and flow logic treat the cell as a water source. Vanilla reads {@link BlockLiquid#LEVEL} from any
 * water-material state, so implementors must include {@link #LEVEL} in their state container and keep it at 0.
 * Water in the cell is drawn by the client render mixin.
 */
public interface IWaterloggable {

    PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");
    PropertyInteger LEVEL = BlockLiquid.LEVEL;

    boolean isWaterlogged(IBlockState state);

    /**
     * @return the state with water added or removed, or {@code null} if this block can't change its water
     * (for example seagrass, which only exists in water).
     */
    default IBlockState withWaterlogged(IBlockState state, boolean waterlogged) {
        return state.getPropertyKeys().contains(WATERLOGGED) ? state.withProperty(WATERLOGGED, waterlogged) : null;
    }
}
