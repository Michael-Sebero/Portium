package com.michaelsebero.portium.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

/** A plain item that survives fire and lava (netherite scrap and ingots, 1.16). */
public class ItemFireproof extends Item implements IFireproofItem {

    public ItemFireproof(CreativeTabs tab) {
        this.setCreativeTab(tab);
    }
}
