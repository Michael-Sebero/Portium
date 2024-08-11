package com.michaelsebero.portium.block.coral;

import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.block.water.WaterloggableHelper;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * Shared behaviour of coral plants and coral fans: no collision, silk-touch-only drops,
 * optional waterlogging, and live variants that dry out into dead ones.
 */
@SuppressWarnings("deprecation")
public abstract class BlockCoralPlantBase extends Block implements ICoral, IWaterloggable {

    private final CoralType type;
    @Nullable
    private final Block deadBlock;
    private final Material dryMaterial;

    protected BlockCoralPlantBase(CoralType type, @Nullable Block deadBlock) {
        super(deadBlock == null ? Material.ROCK : Material.PLANTS, deadBlock == null ? MapColor.GRAY : type.getMapColor());
        this.type = type;
        this.deadBlock = deadBlock;
        this.dryMaterial = this.material;
        this.setHardness(0.0F);
        this.setSoundType(deadBlock == null ? SoundType.STONE : PortiumSoundTypes.WET_GRASS);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public CoralType getCoralType() {
        return this.type;
    }

    @Override
    public boolean isDead() {
        return this.deadBlock == null;
    }

    @Nullable
    @Override
    public Block getDeadBlock() {
        return this.deadBlock;
    }

    @Override
    public boolean isWaterlogged(IBlockState state) {
        return state.getValue(WATERLOGGED);
    }

    // ---- Water ----

    @Nonnull
    @Override
    public Material getMaterial(IBlockState state) {
        return WaterloggableHelper.getMaterial(state, this.dryMaterial);
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

    // ---- Drying out ----

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        this.scheduleDryingCheck(world, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!this.canBlockStay(world, pos, state)) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
            return;
        }
        this.scheduleDryingCheck(world, pos, state);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        this.scheduleDryingCheck(world, pos, state);
    }

    private void scheduleDryingCheck(World world, BlockPos pos, IBlockState state) {
        if (this.deadBlock != null && !this.isWaterlogged(state)) {
            world.scheduleUpdate(pos, this, ICoral.dryingDelay(world.rand));
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (this.deadBlock != null && !this.isWaterlogged(state)) {
            world.setBlockState(pos, this.copyToDead(state), 2);
        }
    }

    /** The dead block in the same shape and orientation. */
    protected IBlockState copyToDead(IBlockState state) {
        assert this.deadBlock != null;
        return this.deadBlock.getDefaultState().withProperty(WATERLOGGED, false);
    }

    public abstract boolean canBlockStay(World world, BlockPos pos, IBlockState state);

    // ---- Shape ----

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Nonnull
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT || (layer == BlockRenderLayer.TRANSLUCENT && this.isWaterlogged(state));
    }

    // ---- Drops: silk touch only ----

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Nonnull
    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock(@Nonnull IBlockState state, net.minecraft.util.math.RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
        return new ItemStack(this);
    }
}
