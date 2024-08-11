package com.michaelsebero.portium.compat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * What a 1.13 item id means in 1.12: an item and metadata (1.13's {@code minecraft:red_wool} is {@code wool:14}),
 * and for a few ids an NBT tag (spawn eggs). {@link #ANY_META} matches every metadata value.
 */
public final class LegacyItem implements Predicate<ItemStack> {

    public static final int ANY_META = -1;

    private final Item item;
    private final int meta;
    @Nullable
    private final NBTTagCompound tag;

    public LegacyItem(Item item, int meta, @Nullable NBTTagCompound tag) {
        this.item = item;
        this.meta = meta;
        this.tag = tag;
    }

    public Item getItem() {
        return this.item;
    }

    public int getMeta() {
        return this.meta;
    }

    @Nullable
    public NBTTagCompound getTag() {
        return this.tag;
    }

    public ItemStack createStack(int count) {
        ItemStack stack = new ItemStack(this.item, count, Math.max(this.meta, 0));
        if (this.tag != null) stack.setTagCompound(this.tag.copy());
        return stack;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.getItem() != this.item) return false;
        if (this.meta != ANY_META && stack.getMetadata() != this.meta) return false;
        return this.tag == null || stack.hasTagCompound() && net.minecraft.nbt.NBTUtil.areNBTEquals(this.tag, stack.getTagCompound(), true);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LegacyItem)) return false;
        LegacyItem that = (LegacyItem) o;
        return that.item == this.item && that.meta == this.meta && java.util.Objects.equals(that.tag, this.tag);
    }

    @Override
    public int hashCode() {
        return this.item.hashCode() * 31 + this.meta;
    }
}
