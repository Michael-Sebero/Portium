package com.michaelsebero.portium.mods116.adapter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A 1.16 ingredient on 1.12's: 1.16's lists items and tags and reads them when first asked, as 1.12 Forge's ore
 * dictionary ingredient does, so an ingredient made before tags load (in a static initialiser) still fills in. They're
 * read again after each data reload, when tags may have changed.
 * <p>
 * Stacks match by item and metadata (a wildcard taking any), as 1.12's do: Portium's 1.16 items can be 1.12 items with
 * metadata (white wool). Forge's NBT and compound ingredients bring a matcher of their own.
 */
public final class Ingredient116 extends Ingredient {

    private static volatile int generation;

    /** The stacks it takes (1.12 stacks, typed loosely for the 1.16 glue that supplies them). */
    private final Supplier<? extends Collection<?>> source;
    /** 1.16's JSON for it, for mods that write recipes out; typed loosely for the same reason. */
    private final Supplier<?> json;
    /** No item lists at all, which 1.16 calls empty even before tags load. */
    private final boolean noLists;
    /** How a stack is matched, where not by item and metadata alone (Forge's NBT and compound ingredients). */
    @Nullable
    private final Predicate<ItemStack> matcher;

    private int readAt = -1;
    private ItemStack[] stacks = new ItemStack[0];
    private ItemStack[] shown = new ItemStack[0];
    @Nullable
    private IntList packed;

    private Ingredient116(Supplier<? extends Collection<?>> source, Supplier<?> json, boolean noLists, @Nullable Predicate<ItemStack> matcher) {
        super(0);
        this.source = source;
        this.json = json;
        this.noLists = noLists;
        this.matcher = matcher;
    }

    public static Ingredient of(Supplier<? extends Collection<?>> stacks, Supplier<?> json, boolean noLists, @Nullable Predicate<ItemStack> matcher) {
        return new Ingredient116(stacks, json, noLists, matcher);
    }

    /** Tags were reloaded: every 1.16 ingredient reads its lists again. */
    public static void reloaded() {
        generation++;
    }

    private synchronized void read() {
        int now = generation;
        if (this.readAt == now) return;
        List<ItemStack> found = new ArrayList<>();
        for (Object value : this.source.get()) {
            if (!(value instanceof ItemStack) || ((ItemStack) value).isEmpty()) continue;
            ItemStack stack = (ItemStack) value;
            boolean seen = false;
            for (ItemStack other : found) {
                if (other.getItem() == stack.getItem() && other.getMetadata() == stack.getMetadata()
                        && ItemStack.areItemStackTagsEqual(other, stack)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) found.add(stack);
        }
        NonNullList<ItemStack> shown = NonNullList.create();
        for (ItemStack stack : found) {
            if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) stack.getItem().getSubItems(CreativeTabs.SEARCH, shown);
            else shown.add(stack);
        }
        this.stacks = found.toArray(new ItemStack[0]);
        this.shown = shown.toArray(new ItemStack[0]);
        this.packed = null;
        this.readAt = now;
    }

    @Override
    public ItemStack[] getMatchingStacks() {
        read();
        return this.shown;
    }

    @Override
    public boolean apply(@Nullable ItemStack input) {
        if (input == null) return false;
        if (this.matcher != null) return this.matcher.test(input);
        read();
        if (this.stacks.length == 0) return input.isEmpty();
        for (ItemStack stack : this.stacks) {
            if (OreDictionary.itemMatches(stack, input, false)) return true;
        }
        return false;
    }

    @Override
    public IntList getValidItemStacksPacked() {
        read();
        if (this.packed == null) {
            IntList packed = new IntArrayList(this.shown.length);
            for (ItemStack stack : this.shown) packed.add(RecipeItemHelper.pack(stack));
            packed.sort(IntComparators.NATURAL_COMPARATOR);
            this.packed = packed;
        }
        return this.packed;
    }

    @Override
    protected void invalidate() {
        this.readAt = -1;
        this.packed = null;
    }

    /** Not one 1.12's recipe book may count by packed stacks alone: tags can hold damageable items. */
    @Override
    public boolean isSimple() {
        return false;
    }

    /** 1.16's {@code hasNoMatchingItems}: made of no item lists. */
    public boolean portium$hasNoLists() {
        return this.noLists;
    }

    /** 1.16's {@code serialize}: a JSON element. */
    public Object portium$json() {
        return this.json.get();
    }
}
