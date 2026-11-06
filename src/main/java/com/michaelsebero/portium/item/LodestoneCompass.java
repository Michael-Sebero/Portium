package com.michaelsebero.portium.item;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * The lodestone compass (1.16): a compass used on a lodestone points at it, in that dimension, as long as the
 * lodestone stands. Same NBT as 1.16 ({@code LodestonePos}, {@code LodestoneDimension}, {@code LodestoneTracked});
 * dimensions are named the 1.16 way for vanilla's three.
 */
public final class LodestoneCompass {

    public static boolean hasLodestone(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && (tag.hasKey("LodestoneDimension") || tag.hasKey("LodestonePos"));
    }

    public static String dimensionKey(World world) {
        DimensionType type = world.provider.getDimensionType();
        if (type == DimensionType.OVERWORLD) return "minecraft:overworld";
        if (type == DimensionType.NETHER) return "minecraft:the_nether";
        if (type == DimensionType.THE_END) return "minecraft:the_end";
        return "portium:dimension_" + world.provider.getDimension();
    }

    /** The lodestone the compass points at, if it's in this world. */
    @Nullable
    public static BlockPos target(World world, @Nullable NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("LodestonePos", 10) || !tag.hasKey("LodestoneDimension", 8)) return null;
        if (!dimensionKey(world).equals(tag.getString("LodestoneDimension"))) return null;
        return NBTUtil.getPosFromTag(tag.getCompoundTag("LodestonePos"));
    }

    /** A compass used on a lodestone locks onto it (a stack of them gives one locked compass). */
    public static EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand) {
        world.playSound(null, pos, PortiumSounds.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        ItemStack stack = player.getHeldItem(hand);
        boolean single = !player.capabilities.isCreativeMode && stack.getCount() == 1;
        if (single) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            write(world, pos, stack.getTagCompound());
        } else {
            ItemStack locked = new ItemStack(Items.COMPASS, 1);
            NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound().copy() : new NBTTagCompound();
            locked.setTagCompound(tag);
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            write(world, pos, tag);
            if (!player.inventory.addItemStackToInventory(locked)) player.dropItem(locked, false);
        }
        return EnumActionResult.SUCCESS;
    }

    private static void write(World world, BlockPos pos, NBTTagCompound tag) {
        tag.setTag("LodestonePos", NBTUtil.createPosTag(pos));
        tag.setString("LodestoneDimension", dimensionKey(world));
        tag.setBoolean("LodestoneTracked", true);
    }

    /** Each tick in an inventory (server): a tracked compass whose lodestone is gone stops pointing. */
    public static void inventoryTick(ItemStack stack, World world) {
        if (world.isRemote || !hasLodestone(stack)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("LodestoneTracked") && !tag.getBoolean("LodestoneTracked")) return;
        BlockPos pos = target(world, tag);
        // 1.16 asks its points of interest, saved with the chunk; 1.12 can only look in loaded chunks.
        if (pos != null && world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() != PortiumBlocks.LODESTONE) tag.removeTag("LodestonePos");
    }

    private LodestoneCompass() {}
}
