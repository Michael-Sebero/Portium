package com.michaelsebero.portium.block.plant;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * A flower pot holding one of Portium's plants, as 1.13 has a block per potted plant ({@code potted_cornflower}).
 * 1.12's own pot keeps its plant in a tile entity and only takes 1.12's plants; {@code handler.FlowerPots} turns an
 * empty one into this when given a Portium plant. Using it with anything that isn't a plant takes the plant out.
 */
public class BlockPortiumFlowerPot extends Block {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.375D, 0.6875D);

    private final Block plant;

    public BlockPortiumFlowerPot(Block plant) {
        super(Material.CIRCUITS);
        this.plant = plant;
        this.setHardness(0.0F);
    }

    public Block getPlant() {
        return this.plant;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        // 1.13: holding a plant that goes in pots does nothing to a full pot.
        if (FlowerPots.isPottable(held)) return true;
        ItemStack taken = new ItemStack(this.plant);
        if (held.isEmpty()) {
            player.setHeldItem(hand, taken);
        } else if (!player.addItemStackToInventory(taken)) {
            player.dropItem(taken, false);
        }
        world.setBlockState(pos, Blocks.FLOWER_POT.getDefaultState(), 3);
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(Items.FLOWER_POT));
        drops.add(new ItemStack(this.plant));
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this.plant);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.FLOWER_POT;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState below = world.getBlockState(pos.down());
        return super.canPlaceBlockAt(world, pos) && (below.isTopSolid() || below.getBlockFaceShape(world, pos.down(), EnumFacing.UP) == BlockFaceShape.SOLID);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        IBlockState below = world.getBlockState(pos.down());
        if (!below.isTopSolid() && below.getBlockFaceShape(world, pos.down(), EnumFacing.UP) != BlockFaceShape.SOLID) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
