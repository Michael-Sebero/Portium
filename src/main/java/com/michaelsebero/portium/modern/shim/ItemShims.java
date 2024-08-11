package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.mc.item.Item$Properties;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import javax.annotation.Nullable;

/** 1.13's item APIs on 1.12 items. */
public final class ItemShims {

    /** Properties of a 1.13 item whose 1.12 constructor is running on this thread. */
    private static final ThreadLocal<Item$Properties> PENDING = new ThreadLocal<>();

    /** For {@code new Item(Properties)} and its kin: sets the properties aside for 1.12's constructor to apply. */
    public static void prepare(Item$Properties properties) {
        PENDING.set(properties);
    }

    /** The properties set aside for the item being made, if it's a 1.13 item; clears them. */
    @Nullable
    public static Item$Properties takePending() {
        Item$Properties properties = PENDING.get();
        PENDING.remove();
        return properties;
    }

    /** For the glue: Forge 1.13's {@code addToolType}, by the tool type's name. */
    public static Object addToolType(Object properties, String tool, int level) {
        return ((Item$Properties) properties).portium$addToolType(tool, level);
    }

    /** A creative tab's index for 1.12: Forge 1.13's -1, for the next free one, made real. */
    public static int groupIndex(int index) {
        return index == -1 ? CreativeTabs.getNextID() : index;
    }

    /** An item provider's item, for 1.12 constructors that take an item. */
    public static Item asItem(@Nullable IItemProvider provider) {
        return provider == null ? Items.AIR : provider.asItem();
    }

    private ItemShims() {}
}
