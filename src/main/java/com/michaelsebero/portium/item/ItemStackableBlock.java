package com.michaelsebero.portium.item;

import com.michaelsebero.portium.block.IStackableBlock;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Places an {@link IStackableBlock}, or adds one to an existing stack that was clicked or is in front of the click.
 */
public class ItemStackableBlock extends ItemBlock {

    public ItemStackableBlock(Block block) {
        super(block);
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty() && player.canPlayerEdit(pos, facing, stack)) {
            if (this.tryAdd(player, world, pos, stack) || this.tryAdd(player, world, pos.offset(facing), stack)) {
                return EnumActionResult.SUCCESS;
            }
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    private boolean tryAdd(EntityPlayer player, World world, BlockPos pos, ItemStack stack) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != this.block) return false;
        IStackableBlock stackable = (IStackableBlock) this.block;
        PropertyInteger count = stackable.getCountProperty();
        if (state.getValue(count) >= stackable.getMaxCount()) return false;

        IBlockState grown = state.withProperty(count, state.getValue(count) + 1);
        if (!world.setBlockState(pos, grown, 3)) return false;
        SoundType sound = grown.getBlock().getSoundType(grown, world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
        }
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        return true;
    }
}
