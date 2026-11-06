package com.michaelsebero.portium.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** A sign of a newer wood: placed standing on a top face (turned to the player) or on a wall, then edited, as 1.12's. */
public class ItemPortiumSign extends Item {

    private final Block standing;
    private final Block wall;

    public ItemPortiumSign(Block standing, Block wall) {
        this.standing = standing;
        this.wall = wall;
        this.maxStackSize = 16;
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState clicked = world.getBlockState(pos);
        boolean replaceable = clicked.getBlock().isReplaceable(world, pos);
        if (facing == EnumFacing.DOWN || !clicked.getMaterial().isSolid() && !replaceable || replaceable && facing != EnumFacing.UP) return EnumActionResult.FAIL;
        BlockPos at = replaceable ? pos : pos.offset(facing);
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(at, facing, stack) || !this.standing.canPlaceBlockAt(world, at)) return EnumActionResult.FAIL;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        if (facing == EnumFacing.UP) {
            int rotation = MathHelper.floor((player.rotationYaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15;
            world.setBlockState(at, this.standing.getDefaultState().withProperty(BlockStandingSign.ROTATION, rotation), 11);
        } else {
            world.setBlockState(at, this.wall.getDefaultState().withProperty(BlockWallSign.FACING, facing), 11);
        }
        TileEntity tile = world.getTileEntity(at);
        if (tile instanceof TileEntitySign && !ItemBlock.setTileEntityNBT(world, player, at, stack)) player.openEditSign((TileEntitySign) tile);
        if (player instanceof EntityPlayerMP) CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, at, stack);
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }
}
