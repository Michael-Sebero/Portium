package com.michaelsebero.portium.mods116.shim;

import net.minecraft.creativetab.CreativeTabs;

/** Forge 1.16's item methods that 1.12 has another way, for 1.16 code. */
public final class ItemShims116 {

    /** The index the next creative tab takes: 1.12's own count of them. */
    public static int groupCount() {
        return CreativeTabs.getNextID();
    }

    /** 1.14's {@code item.isFood()}: 1.12's food items, and 1.16 mods' items made with food. */
    public static boolean isFood(net.minecraft.item.Item item) {
        return item instanceof net.minecraft.item.ItemFood
                || item instanceof com.michaelsebero.portium.modern.adapter.ModernItem && ((com.michaelsebero.portium.modern.adapter.ModernItem) item).portium$getFood() != null;
    }

    // 1.13's stacks of an item or block, for constructor references (ItemStack::new): flattened ones are 1.12 variants.

    public static net.minecraft.item.ItemStack stack(com.michaelsebero.portium.modern.mc.util.IItemProvider item) {
        return stack(item, 1);
    }

    public static net.minecraft.item.ItemStack stack(com.michaelsebero.portium.modern.mc.util.IItemProvider item, int count) {
        return new net.minecraft.item.ItemStack(com.michaelsebero.portium.modern.flat.Flattening.stackItem(item), count,
                com.michaelsebero.portium.modern.flat.Flattening.stackMeta(item));
    }

    public static net.minecraft.item.ItemStack stack(com.michaelsebero.portium.modern.mc.util.IItemProvider item, int count, net.minecraft.nbt.NBTTagCompound nbt) {
        return new net.minecraft.item.ItemStack(com.michaelsebero.portium.modern.flat.Flattening.stackItem(item), count,
                com.michaelsebero.portium.modern.flat.Flattening.stackMeta(item), nbt);
    }

    private ItemShims116() {}
}
