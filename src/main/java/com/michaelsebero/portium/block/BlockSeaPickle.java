package com.michaelsebero.portium.block;

import com.michaelsebero.portium.block.coral.BlockCoralBlock;
import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.block.water.WaterloggableHelper;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Sea pickles: 1-4 per block, glowing only while waterlogged. Bone meal on a live coral block spreads them (1.13).
 */
@SuppressWarnings("deprecation")
public class BlockSeaPickle extends Block implements IWaterloggable, IGrowable, IStackableBlock {

    public static final PropertyInteger PICKLES = PropertyInteger.create("pickles", 1, 4);

    private static final AxisAlignedBB ONE_AABB = new AxisAlignedBB(6 / 16.0, 0, 6 / 16.0, 10 / 16.0, 6 / 16.0, 10 / 16.0);
    private static final AxisAlignedBB TWO_AABB = new AxisAlignedBB(3 / 16.0, 0, 3 / 16.0, 13 / 16.0, 6 / 16.0, 13 / 16.0);
    private static final AxisAlignedBB THREE_AABB = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 6 / 16.0, 14 / 16.0);
    private static final AxisAlignedBB FOUR_AABB = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 7 / 16.0, 14 / 16.0);

    public BlockSeaPickle() {
        super(Material.PLANTS, MapColor.GREEN);
        this.setDefaultState(this.blockState.getBaseState().withProperty(PICKLES, 1).withProperty(WATERLOGGED, true).withProperty(LEVEL, 0));
        this.setSoundType(SoundType.SLIME);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public PropertyInteger getCountProperty() {
        return PICKLES;
    }

    @Override
    public boolean isWaterlogged(IBlockState state) {
        return state.getValue(WATERLOGGED);
    }

    @Nonnull
    @Override
    public Material getMaterial(IBlockState state) {
        return WaterloggableHelper.getMaterial(state, Material.PLANTS);
    }

    @Override
    public int getLightOpacity(IBlockState state) {
        return WaterloggableHelper.getLightOpacity(state, 0);
    }

    @Override
    public int getLightValue(IBlockState state) {
        return this.isWaterlogged(state) ? 3 + 3 * state.getValue(PICKLES) : 0;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        WaterloggableHelper.breakBlock(world, pos, state);
    }

    // ---- Placement ----

    private static boolean hasSupport(IBlockAccess world, BlockPos pos) {
        BlockPos below = pos.down();
        BlockFaceShape shape = world.getBlockState(below).getBlockFaceShape(world, below, EnumFacing.UP);
        return shape != BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && hasSupport(world, pos);
    }

    @Nonnull
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(WATERLOGGED, WaterloggableHelper.shouldPlaceWaterlogged(world, pos));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!hasSupport(world, pos)) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        return state.getValue(PICKLES);
    }

    // ---- Bone meal ----

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return this.isWaterlogged(state);
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    private static boolean isLiveCoralBlock(IBlockState state) {
        return state.getBlock() instanceof BlockCoralBlock && !((BlockCoralBlock) state.getBlock()).isDead();
    }

    /**
     * 1.13 behaviour: on a live coral block, spread pickles over a diamond of nearby coral (rows 1, 3, 5, 3, 1 wide)
     * one layer below and level with this pickle, then fill this block to four.
     */
    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        if (!this.isWaterlogged(state) || !isLiveCoralBlock(world.getBlockState(pos.down()))) return;
        int rowWidth = 1;
        int rowOffset = 0;
        for (int row = 0; row < 5; ++row) {
            for (int i = 0; i < rowWidth; ++i) {
                for (int y = pos.getY() - 1; y <= pos.getY(); ++y) {
                    BlockPos target = new BlockPos(pos.getX() - 2 + row, y, pos.getZ() - rowOffset + i);
                    if (target.equals(pos) || rand.nextInt(6) != 0) continue;
                    if (WaterUtil.isPlainWaterSource(world.getBlockState(target)) && isLiveCoralBlock(world.getBlockState(target.down()))) {
                        world.setBlockState(target, this.getDefaultState().withProperty(PICKLES, rand.nextInt(4) + 1), 3);
                    }
                }
            }
            if (row < 2) {
                rowWidth += 2;
                ++rowOffset;
            } else {
                rowWidth -= 2;
                --rowOffset;
            }
        }
        world.setBlockState(pos, state.withProperty(PICKLES, 4), 2);
    }

    // ---- Shape ----

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(PICKLES)) {
            case 1: return ONE_AABB;
            case 2: return TWO_AABB;
            case 3: return THREE_AABB;
            default: return FOUR_AABB;
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Nonnull
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Nonnull
    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT || (layer == BlockRenderLayer.TRANSLUCENT && this.isWaterlogged(state));
    }

    // pickles-1 in bits 0-1, waterlogged in bit 2.
    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(PICKLES) - 1) | (state.getValue(WATERLOGGED) ? 4 : 0);
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(PICKLES, (meta & 3) + 1).withProperty(WATERLOGGED, (meta & 4) != 0);
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PICKLES, WATERLOGGED, LEVEL);
    }
}
