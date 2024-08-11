package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * What 1.16 code makes or extends in place of 1.16's shaped recipe (Create's mechanical crafting recipe extends it):
 * 1.12's, which has the same grid fields, made with 1.16's id first.
 */
public class ShapedRecipe116 extends ShapedRecipes {

    private final ResourceLocation id;

    public ShapedRecipe116(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(group, width, height, ingredients, result);
        this.id = id;
        setRegistryName(id);
    }

    @Srg("func_199560_c")
    public ResourceLocation getId() {
        return this.id;
    }
}
