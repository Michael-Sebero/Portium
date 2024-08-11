package com.michaelsebero.portium.block;

import com.michaelsebero.portium.block.water.BlockWaterPlant;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BlockSeagrass extends BlockWaterPlant implements IGrowable {

    private static final AxisAlignedBB AABB = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 12 / 16.0, 14 / 16.0);

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return WaterUtil.isPlainWaterSource(world.getBlockState(pos.up()));
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        if (!this.canGrow(world, pos, state, false)) return;
        IBlockState lower = PortiumBlocks.TALL_SEAGRASS.getDefaultState();
        world.setBlockState(pos, lower, 2);
        world.setBlockState(pos.up(), lower.withProperty(BlockTallSeagrass.HALF, BlockDoublePlant.EnumBlockHalf.UPPER), 2);
    }

    @Override
    public boolean isShearable(@Nonnull ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nonnull ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL);
    }
}
