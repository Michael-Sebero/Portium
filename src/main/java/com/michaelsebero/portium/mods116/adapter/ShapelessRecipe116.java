package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/** What 1.16 code makes or extends in place of 1.16's shapeless recipe: 1.12's, made with 1.16's id first. */
public class ShapelessRecipe116 extends ShapelessRecipes {

    private final ResourceLocation id;

    public ShapelessRecipe116(ResourceLocation id, String group, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(group, result, ingredients);
        this.id = id;
        setRegistryName(id);
    }

    @Srg("func_199560_c")
    public ResourceLocation getId() {
        return this.id;
    }
}
