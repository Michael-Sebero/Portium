package com.michaelsebero.portium.modern.flat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** A 1.13 item that 1.12 has only as an item's metadata (see {@link Flattening}): unregistered, standing for it. */
public class FlattenedItem extends Item implements FlattenedItemView {

    private final ResourceLocation name;
    private final Item base;
    private final int meta;

    FlattenedItem(String name, Item base, int meta) {
        this.name = new ResourceLocation(name);
        this.base = base;
        this.meta = meta;
        setRegistryName(this.name);
        setMaxStackSize(base.getItemStackLimit(new ItemStack(base, 1, meta)));
    }

    @Override
    public Item base() {
        return this.base;
    }

    @Override
    public int meta() {
        return this.meta;
    }

    @Override
    public String getTranslationKey() {
        return new ItemStack(this.base, 1, this.meta).getTranslationKey();
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getTranslationKey();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return new ItemStack(this.base, 1, this.meta).getDisplayName();
    }

    @Override
    public String toString() {
        return this.name.toString();
    }
}
