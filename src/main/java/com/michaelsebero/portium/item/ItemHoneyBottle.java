package com.michaelsebero.portium.item;

import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** The honey bottle (1.15): drunk over 2 seconds for 3 hunger shanks, curing poison; leaves the bottle. Stacks to 16. */
public class ItemHoneyBottle extends ItemFood implements ICustomDrinkSound {

    public ItemHoneyBottle() {
        super(6, 0.1F, false);
        this.setMaxStackSize(16);
        this.setContainerItem(Items.GLASS_BOTTLE);
        this.setCreativeTab(CreativeTabs.FOOD);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        super.onItemUseFinish(stack, world, entity);
        if (!world.isRemote) entity.removePotionEffect(MobEffects.POISON);
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (entity instanceof EntityPlayer && !((EntityPlayer) entity).capabilities.isCreativeMode) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            EntityPlayer player = (EntityPlayer) entity;
            if (!player.inventory.addItemStackToInventory(bottle)) player.dropItem(bottle, false);
        }
        return stack;
    }

    /** Drunk whether hungry or not (1.15 starts drinking straight away, unlike eating). */
    @Override
    public net.minecraft.util.ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, net.minecraft.util.EnumHand hand) {
        player.setActiveHand(hand);
        return new net.minecraft.util.ActionResult<>(net.minecraft.util.EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public SoundEvent getDrinkSound() {
        return PortiumSounds.ITEM_HONEY_BOTTLE_DRINK;
    }
}
