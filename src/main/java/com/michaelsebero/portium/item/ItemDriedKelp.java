package com.michaelsebero.portium.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;

/** Dried kelp (1.13): one hunger point, eaten twice as fast as other food. */
public class ItemDriedKelp extends ItemFood {

    public ItemDriedKelp() {
        super(1, 0.3F, false);
        this.setCreativeTab(CreativeTabs.FOOD);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 16;
    }
}
