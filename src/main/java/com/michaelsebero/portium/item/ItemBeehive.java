package com.michaelsebero.portium.item;

import com.michaelsebero.portium.block.bee.BlockBeehive;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A bee nest or beehive item; 1.15's {@code BlockStateTag.honey_level} sets the placed hive's honey (its bees come with {@code BlockEntityTag}). */
public class ItemBeehive extends ItemBlock {

    public ItemBeehive(Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
                                IBlockState newState) {
        NBTTagCompound tag = stack.getSubCompound("BlockStateTag");
        if (tag != null && tag.hasKey("honey_level") && newState.getBlock() instanceof BlockBeehive) {
            try {
                int honey = Integer.parseInt(tag.getString("honey_level").isEmpty() ? String.valueOf(tag.getInteger("honey_level")) : tag.getString("honey_level"));
                newState = newState.withProperty(BlockBeehive.HONEY_LEVEL, Math.max(0, Math.min(5, honey)));
            } catch (NumberFormatException ignored) {
            }
        }
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
}
