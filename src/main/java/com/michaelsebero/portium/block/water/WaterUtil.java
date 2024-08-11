package com.michaelsebero.portium.block.water;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class WaterUtil {

    /** The water source state placed back when a waterlogged block is removed. */
    public static IBlockState waterSource() {
        return Blocks.WATER.getDefaultState();
    }

    /** How much light a block of water absorbs: 1 with 1.13's clearer water, otherwise 1.12's 3. */
    public static int waterLightOpacity() {
        return com.michaelsebero.portium.PortiumConfig.mechanics.clearerWater ? 1 : 3;
    }

    /** True for still water sources, bubble columns and waterlogged Portium blocks. */
    public static boolean isWaterSource(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof IWaterloggable) return ((IWaterloggable) block).isWaterlogged(state);
        return state.getMaterial() == Material.WATER && block instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    /** True only for a still water source block: the cells plants and bubble columns may grow into. */
    public static boolean isPlainWaterSource(IBlockState state) {
        return state.getBlock() == Blocks.WATER && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    public static boolean isWaterSource(IBlockAccess world, BlockPos pos) {
        return isWaterSource(world.getBlockState(pos));
    }

    /** True for any water, flowing or still, including waterlogged Portium blocks. */
    public static boolean isWater(IBlockState state) {
        return state.getMaterial() == Material.WATER;
    }

    /** Whether any of the six neighbours holds water. Live coral blocks die without this. */
    public static boolean isTouchingWater(IBlockAccess world, BlockPos pos) {
        BlockPos.PooledMutableBlockPos mutable = BlockPos.PooledMutableBlockPos.retain();
        try {
            for (EnumFacing facing : EnumFacing.VALUES) {
                mutable.setPos(pos).move(facing);
                if (Waterlogging.hasWater(world, mutable)) return true;
            }
            return false;
        } finally {
            mutable.release();
        }
    }

    /**
     * Removes a waterlogged block the way 1.13 does: the cell keeps its water.
     * Call from {@link Block#breakBlock} once the block has been replaced by air.
     */
    public static void restoreWaterIfCleared(World world, BlockPos pos) {
        if (!world.isRemote && world.isAirBlock(pos) && !world.provider.doesWaterVaporize()) {
            world.setBlockState(pos, waterSource(), 3);
        }
    }

    private WaterUtil() {}
}
