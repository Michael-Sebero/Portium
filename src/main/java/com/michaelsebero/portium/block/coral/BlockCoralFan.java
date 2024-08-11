package com.michaelsebero.portium.block.coral;

import com.michaelsebero.portium.block.water.WaterloggableHelper;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A coral fan. {@code facing=up} is the floor fan; horizontal facings are 1.13's wall fans,
 * attached to the block behind them.
 */
public class BlockCoralFan extends BlockCoralPlantBase {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", f -> f != EnumFacing.DOWN);

    private static final AxisAlignedBB FLOOR_AABB = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 4 / 16.0, 14 / 16.0);
    private static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(0, 4 / 16.0, 5 / 16.0, 1, 12 / 16.0, 1);
    private static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(0, 4 / 16.0, 0, 1, 12 / 16.0, 11 / 16.0);
    private static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(5 / 16.0, 4 / 16.0, 0, 1, 12 / 16.0, 1);
    private static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(0, 4 / 16.0, 0, 11 / 16.0, 12 / 16.0, 1);

    public BlockCoralFan(CoralType type, @Nullable Block deadBlock) {
        super(type, deadBlock);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP).withProperty(WATERLOGGED, false).withProperty(LEVEL, 0));
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case NORTH: return NORTH_AABB;
            case SOUTH: return SOUTH_AABB;
            case WEST: return WEST_AABB;
            case EAST: return EAST_AABB;
            default: return FLOOR_AABB;
        }
    }

    private static boolean canAttach(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).getBlockFaceShape(world, support, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return side != EnumFacing.DOWN && super.canPlaceBlockAt(world, pos) && canAttach(world, pos, side);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (!super.canPlaceBlockAt(world, pos)) return false;
        for (EnumFacing facing : FACING.getAllowedValues()) {
            if (canAttach(world, pos, facing)) return true;
        }
        return false;
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return canAttach(world, pos, state.getValue(FACING));
    }

    @Nonnull
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing attach = facing;
        if (attach == EnumFacing.DOWN || !canAttach(world, pos, attach)) {
            attach = canAttach(world, pos, EnumFacing.UP) ? EnumFacing.UP : placer.getHorizontalFacing().getOpposite();
        }
        return this.getDefaultState().withProperty(FACING, attach).withProperty(WATERLOGGED, WaterloggableHelper.shouldPlaceWaterlogged(world, pos));
    }

    @Override
    protected IBlockState copyToDead(IBlockState state) {
        return super.copyToDead(state).withProperty(FACING, state.getValue(FACING));
    }

    @Nonnull
    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Nonnull
    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // facing: up=0, north=1, south=2, west=3, east=4; bit 3 is waterlogged.
    @Override
    public int getMetaFromState(IBlockState state) {
        int facing = state.getValue(FACING) == EnumFacing.UP ? 0 : state.getValue(FACING).getIndex() - 1;
        return facing | (state.getValue(WATERLOGGED) ? 8 : 0);
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        int facing = meta & 7;
        EnumFacing direction = facing == 0 || facing > 4 ? EnumFacing.UP : EnumFacing.byIndex(facing + 1);
        return this.getDefaultState().withProperty(FACING, direction).withProperty(WATERLOGGED, (meta & 8) != 0);
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, WATERLOGGED, LEVEL);
    }
}
