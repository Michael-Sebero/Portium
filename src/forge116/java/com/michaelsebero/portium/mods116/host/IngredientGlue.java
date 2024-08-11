package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.shim.IngredientShims116;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;

import java.util.ArrayList;
import java.util.List;

/** {@code Ingredient.fromTag}, which takes a 1.16 tag: a named one is read by name from Portium's tags when asked. */
public final class IngredientGlue {

    @SuppressWarnings("unchecked")
    public static Ingredient fromTag(ITag<Item> tag) {
        if (tag instanceof ITag.INamedTag) return (Ingredient) IngredientShims116.fromItemTag(((ITag.INamedTag<Item>) tag).func_230234_a_());
        return (Ingredient) IngredientShims116.ofStacks(() -> {
            List<ItemStack> stacks = new ArrayList<>();
            for (Item item : tag.func_230236_b_()) stacks.add(new ItemStack(item));
            return stacks;
        });
    }

    private IngredientGlue() {}
}
