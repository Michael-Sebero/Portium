package com.michaelsebero.portium.item;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Sweet berries (1.14): food (2 hunger), and used on the ground they plant a sweet berry bush. */
public class ItemSweetBerries extends ItemFood {

    public ItemSweetBerries() {
        super(2, 0.1F, false);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (PortiumBlocks.SWEET_BERRY_BUSH == null) return EnumActionResult.PASS;
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) pos = pos.offset(facing);
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || !player.canPlayerEdit(pos, facing, stack) || !world.mayPlace(PortiumBlocks.SWEET_BERRY_BUSH, pos, false, facing, player)) {
            return EnumActionResult.FAIL;
        }
        IBlockState bush = PortiumBlocks.SWEET_BERRY_BUSH.getDefaultState();
        if (!world.setBlockState(pos, bush, 11)) return EnumActionResult.FAIL;
        SoundType sound = bush.getBlock().getSoundType(bush, world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player instanceof EntityPlayerMP) CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }
}
