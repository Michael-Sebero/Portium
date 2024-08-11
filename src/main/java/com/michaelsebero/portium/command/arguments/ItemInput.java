package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItemNbt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/** An item and its NBT from a command such as {@code give}, written in 1.13's syntax. */
public class ItemInput {

    private final LegacyItem item;
    @Nullable
    private final NBTTagCompound tag;

    public ItemInput(LegacyItem item, @Nullable NBTTagCompound tag) {
        this.item = item;
        this.tag = tag;
    }

    public LegacyItem getItem() {
        return this.item;
    }

    public ItemStack createStack(int count) {
        ItemStack stack = this.item.createStack(count);
        LegacyItemNbt.apply(stack, this.tag);
        return stack;
    }
}
