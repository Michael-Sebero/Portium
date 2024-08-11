package com.michaelsebero.portium.modern.flat;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** A flattened item of a flattened block (see {@link Flattening}): 1.13 code finds the block view behind it. */
public final class FlattenedItemBlock extends ItemBlock implements FlattenedItemView {

    private final ResourceLocation name;
    private final ItemBlock base;
    private final int meta;

    FlattenedItemBlock(String name, FlattenedBlock block, ItemBlock base, int meta) {
        super(block);
        this.name = new ResourceLocation(name);
        this.base = base;
        this.meta = meta;
        setRegistryName(this.name);
    }

    @Override
    public ItemBlock base() {
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
