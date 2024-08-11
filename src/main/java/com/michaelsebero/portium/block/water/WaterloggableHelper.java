package com.michaelsebero.portium.block.water;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Shared behaviour for blocks that can exist both dry and waterlogged (coral, sea pickles, the conduit).
 * Those blocks forward the matching {@link net.minecraft.block.Block} overrides here.
 */
public final class WaterloggableHelper {

    public static Material getMaterial(IBlockState state, Material dry) {
        return state.getValue(IWaterloggable.WATERLOGGED) ? Material.WATER : dry;
    }

    public static int getLightOpacity(IBlockState state, int dryOpacity) {
        return state.getValue(IWaterloggable.WATERLOGGED) ? Math.max(WaterUtil.waterLightOpacity(), dryOpacity) : dryOpacity;
    }

    /** Whether a block placed at {@code pos} should start waterlogged: only when it replaces a still water source. */
    public static boolean shouldPlaceWaterlogged(World world, BlockPos pos) {
        return WaterUtil.isPlainWaterSource(world.getBlockState(pos));
    }

    public static void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (state.getValue(IWaterloggable.WATERLOGGED)) WaterUtil.restoreWaterIfCleared(world, pos);
    }

    private WaterloggableHelper() {}
}
