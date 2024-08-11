package com.michaelsebero.portium.block;

import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.block.water.WaterloggableHelper;
import com.michaelsebero.portium.tile.TileEntityConduit;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * The conduit block. All behaviour lives in {@link TileEntityConduit}; the block itself is waterloggable
 * because the conduit only works inside water.
 */
@SuppressWarnings("deprecation")
public class BlockConduit extends Block implements IWaterloggable {

    private static final AxisAlignedBB AABB = new AxisAlignedBB(5 / 16.0, 5 / 16.0, 5 / 16.0, 11 / 16.0, 11 / 16.0, 11 / 16.0);

    public BlockConduit() {
        super(Material.GLASS, MapColor.DIAMOND);
        this.setDefaultState(this.blockState.getBaseState().withProperty(WATERLOGGED, true).withProperty(LEVEL, 0));
        this.setHardness(3.0F);
        this.setLightLevel(1.0F);
        this.setSoundType(SoundType.GLASS);
        this.setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public boolean isWaterlogged(IBlockState state) {
        return state.getValue(WATERLOGGED);
    }

    @Nonnull
    @Override
    public Material getMaterial(IBlockState state) {
        return WaterloggableHelper.getMaterial(state, Material.GLASS);
    }

    @Override
    public int getLightOpacity(IBlockState state) {
        return WaterloggableHelper.getLightOpacity(state, 0);
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

    @Nonnull
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(WATERLOGGED, WaterloggableHelper.shouldPlaceWaterlogged(world, pos));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityConduit();
    }

    @Nonnull
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.TRANSLUCENT && this.isWaterlogged(state);
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
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
