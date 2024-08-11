package com.michaelsebero.portium.item;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/** A plain item with a rarity colour, for crafting materials like the heart of the sea. */
public class ItemSimple extends Item {

    private final EnumRarity rarity;

    public ItemSimple(EnumRarity rarity) {
        this.rarity = rarity;
    }

    @Nonnull
    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return this.rarity;
    }
}
