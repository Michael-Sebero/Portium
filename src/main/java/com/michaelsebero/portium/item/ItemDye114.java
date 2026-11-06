package com.michaelsebero.portium.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

/**
 * 1.14's own white, blue, brown and black dyes. 1.12's bone meal, lapis, cocoa beans and ink sac stay dyes too (1.12
 * mods' recipes use them); these dye everything they do: recipes (see {@code Dyes114}), sheep and wolf collars.
 */
public class ItemDye114 extends Item {

    private final EnumDyeColor color;

    public ItemDye114(EnumDyeColor color) {
        this.color = color;
        this.setCreativeTab(CreativeTabs.MATERIALS);
    }

    public EnumDyeColor getColor() {
        return this.color;
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        if (!(target instanceof EntitySheep)) return false;
        EntitySheep sheep = (EntitySheep) target;
        if (!sheep.getSheared() && sheep.getFleeceColor() != this.color) {
            if (!player.world.isRemote) sheep.setFleeceColor(this.color);
            stack.shrink(1);
        }
        return true;
    }
}
