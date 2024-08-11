package com.michaelsebero.portium.modern.mc.item;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 1.13's {@code net.minecraft.item.Item.Properties}: what an item is made with. Named with the {@code $} of a nested
 * class so it can stand in for one while 1.12's {@code Item} stays the real thing. Applied to 1.12's item as its
 * constructor finishes (see {@code ItemShims}). 1.13's item groups are 1.12's creative tabs.
 */
public class Item$Properties {

    int maxStackSize = 64;
    int maxDamage;
    @Nullable
    Item containerItem;
    @Nullable
    CreativeTabs group;
    EnumRarity rarity = EnumRarity.COMMON;
    boolean canRepair = true;
    final Map<String, Integer> toolLevels = new HashMap<>();
    @Nullable
    Supplier<?> renderer;

    @Srg("func_200917_a")
    public Item$Properties maxStackSize(int maxStackSize) {
        if (this.maxDamage > 0) throw new RuntimeException("Unable to have damage AND stack.");
        this.maxStackSize = maxStackSize;
        return this;
    }

    @Srg("func_200915_b")
    public Item$Properties defaultMaxDamage(int maxDamage) {
        return this.maxDamage == 0 ? maxDamage(maxDamage) : this;
    }

    @Srg("func_200918_c")
    private Item$Properties maxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
        this.maxStackSize = 1;
        return this;
    }

    @Srg("func_200919_a")
    public Item$Properties containerItem(Item containerItem) {
        this.containerItem = containerItem;
        return this;
    }

    @Srg("func_200916_a")
    public Item$Properties group(CreativeTabs group) {
        this.group = group;
        return this;
    }

    @Srg("func_208103_a")
    public Item$Properties rarity(EnumRarity rarity) {
        this.rarity = rarity;
        return this;
    }

    /** Forge 1.13's. */
    public Item$Properties setNoRepair() {
        this.canRepair = false;
        return this;
    }

    /**
     * Forge 1.13's: the item's own stack renderer, a {@code TileEntityItemStackRenderer}, made on the client by a
     * supplier of a callable.
     */
    public Item$Properties setTEISR(Supplier<?> renderer) {
        this.renderer = renderer;
        return this;
    }

    /** Forge 1.16's name for it. */
    public Item$Properties setISTER(Supplier<?> renderer) {
        return setTEISR(renderer);
    }

    /** Forge 1.13's {@code addToolType}, by the tool type's name (see {@code ItemGlue}). */
    public Item$Properties portium$addToolType(String tool, int level) {
        this.toolLevels.put(tool, level);
        return this;
    }

    public boolean canRepair() {
        return this.canRepair;
    }

    public Map<String, Integer> getToolLevels() {
        return this.toolLevels;
    }

    @Nullable
    public Supplier<?> getRenderer() {
        return this.renderer;
    }

    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    public int getMaxDamage() {
        return this.maxDamage;
    }

    @Nullable
    public Item getContainerItem() {
        return this.containerItem;
    }

    @Nullable
    public CreativeTabs getGroup() {
        return this.group;
    }

    public EnumRarity getRarity() {
        return this.rarity;
    }

    /** 1.14's food (from {@code food(Food)}, which the glue reads into this); null for an item that isn't one. */
    @javax.annotation.Nullable
    private com.michaelsebero.portium.modern.adapter.FoodValues food;

    public Item$Properties portium$food(com.michaelsebero.portium.modern.adapter.FoodValues food) {
        this.food = food;
        return this;
    }

    @javax.annotation.Nullable
    public com.michaelsebero.portium.modern.adapter.FoodValues getFood() {
        return this.food;
    }
}
