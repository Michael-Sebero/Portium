package com.michaelsebero.portium.block.coral;

import com.michaelsebero.portium.block.water.WaterloggableHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The upright coral plant ({@code tube_coral}, {@code dead_tube_coral}, ...).
 */
public class BlockCoralPlant extends BlockCoralPlantBase {

    private static final AxisAlignedBB AABB = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 15 / 16.0, 14 / 16.0);

    public BlockCoralPlant(CoralType type, @Nullable Block deadBlock) {
        super(type, deadBlock);
        this.setDefaultState(this.blockState.getBaseState().withProperty(WATERLOGGED, false).withProperty(LEVEL, 0));
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && world.isSideSolid(pos.down(), EnumFacing.UP);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return world.isSideSolid(pos.down(), EnumFacing.UP);
    }

    @Nonnull
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(WATERLOGGED, WaterloggableHelper.shouldPlaceWaterlogged(world, pos));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(WATERLOGGED) ? 1 : 0;
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(WATERLOGGED, (meta & 1) == 1);
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, WATERLOGGED, LEVEL);
    }
}
