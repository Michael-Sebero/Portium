package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The body of a weeping or twisting vine (1.16): everything but its growing end ({@link BlockNetherVine}). When the
 * block past it is gone, it becomes the end itself; bone meal grows the vine from its end.
 */
public class BlockNetherVinePlant extends Block implements IGrowable, IShearable {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);

    private final BlockNetherVine top;

    public BlockNetherVinePlant(MapColor mapColor, BlockNetherVine top) {
        super(Material.PLANTS, mapColor);
        this.top = top;
        top.setBody(this);
        this.setSoundType(PortiumSoundTypes.NETHER_VINE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

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

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this.top);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!BlockNetherVine.canHangFrom(world, pos, this.top.growth, this.top, this)) {
            world.scheduleUpdate(pos, this, 1);
            return;
        }
        Block next = world.getBlockState(pos.offset(this.top.growth)).getBlock();
        if (next != this && next != this.top) world.setBlockState(pos, this.top.withRandomAge(world.rand));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!BlockNetherVine.canHangFrom(world, pos, this.top.growth, this.top, this)) world.destroyBlock(pos, true);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return false;
    }

    // ---- Growth: bone meal grows the vine from its end ----

    @Nullable
    private BlockPos endOf(World world, BlockPos pos) {
        BlockPos at = pos;
        Block block;
        do {
            at = at.offset(this.top.growth);
            block = world.getBlockState(at).getBlock();
        } while (block == this);
        return block == this.top ? at : null;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        BlockPos end = this.endOf(world, pos);
        return end != null && world.isAirBlock(end.offset(this.top.growth));
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        BlockPos end = this.endOf(world, pos);
        if (end != null) this.top.grow(world, random, end, world.getBlockState(end));
    }

    // ---- Drops: as the end ----

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        BlockNetherVine.dropByChance(drops, this.top, fortune);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this.top);
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this.top));
    }
}
