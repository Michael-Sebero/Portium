package com.michaelsebero.portium.item;

import com.michaelsebero.portium.Tags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.EnumHelper;

/** 1.14's leather horse armour: 3 armour, dyeable like leather armour (the horse wears it in its colour). */
public class ItemLeatherHorseArmor extends Item {

    public static final int DEFAULT_COLOR = 0xA06540;
    private static final String TEXTURE = Tags.MOD_ID + ":textures/entity/horse/armor/horse_armor_leather.png";
    private static HorseArmorType type;

    public ItemLeatherHorseArmor() {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.MISC);
        if (type == null) type = EnumHelper.addHorseArmor("PORTIUM_LEATHER", TEXTURE, 3);
    }

    @Override
    public HorseArmorType getHorseArmorType(ItemStack stack) {
        return type;
    }

    @Override
    public String getHorseArmorTexture(EntityLiving wearer, ItemStack stack) {
        return TEXTURE + "|tint=" + String.format("%06x", getColor(stack));
    }

    /** 1.14: a cauldron with water washes the dye out, a level of water at a time. */
    @Override
    public net.minecraft.util.EnumActionResult onItemUse(net.minecraft.entity.player.EntityPlayer player, net.minecraft.world.World world,
                                                         net.minecraft.util.math.BlockPos pos, net.minecraft.util.EnumHand hand,
                                                         net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        ItemStack stack = player.getHeldItem(hand);
        if (!(state.getBlock() instanceof net.minecraft.block.BlockCauldron) || !hasColor(stack)) return net.minecraft.util.EnumActionResult.PASS;
        int level = state.getValue(net.minecraft.block.BlockCauldron.LEVEL);
        if (level <= 0) return net.minecraft.util.EnumActionResult.PASS;
        if (!world.isRemote) {
            NBTTagCompound display = stack.getSubCompound("display");
            if (display != null) display.removeTag("color");
            ((net.minecraft.block.BlockCauldron) state.getBlock()).setWaterLevel(world, pos, state, level - 1);
            player.addStat(net.minecraft.stats.StatList.ARMOR_CLEANED);
        }
        return net.minecraft.util.EnumActionResult.SUCCESS;
    }

    public static boolean hasColor(ItemStack stack) {
        NBTTagCompound display = stack.getSubCompound("display");
        return display != null && display.hasKey("color", 99);
    }

    public static int getColor(ItemStack stack) {
        NBTTagCompound display = stack.getSubCompound("display");
        return display != null && display.hasKey("color", 99) ? display.getInteger("color") : DEFAULT_COLOR;
    }

    public static void setColor(ItemStack stack, int color) {
        stack.getOrCreateSubCompound("display").setInteger("color", color);
    }
}
