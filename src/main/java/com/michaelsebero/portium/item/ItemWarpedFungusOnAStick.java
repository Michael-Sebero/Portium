package com.michaelsebero.portium.item;

import com.michaelsebero.portium.entity.passive.EntityStrider;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/** 1.16's warped fungus on a stick: steers a saddled strider, and boosts it for 1 durability (of 100); breaks into a fishing rod. */
public class ItemWarpedFungusOnAStick extends Item {

    public ItemWarpedFungusOnAStick() {
        this.setMaxStackSize(1);
        this.setMaxDamage(100);
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean shouldRotateAroundWhenRendering() {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) return new ActionResult<>(EnumActionResult.PASS, stack);
        Entity mount = player.getRidingEntity();
        if (player.isRiding() && mount instanceof EntityStrider && ((EntityStrider) mount).boost()) {
            stack.damageItem(1, player);
            if (stack.isEmpty()) {
                ItemStack rod = new ItemStack(Items.FISHING_ROD);
                rod.setTagCompound(stack.getTagCompound());
                return new ActionResult<>(EnumActionResult.SUCCESS, rod);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
}
