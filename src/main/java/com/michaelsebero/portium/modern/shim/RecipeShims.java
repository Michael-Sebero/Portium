package com.michaelsebero.portium.modern.shim;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** 1.13's recipe manager, answered by 1.12's furnace recipes and crafting manager. */
public final class RecipeShims {

    public static ItemStack smelt(ItemStack input) {
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input);
        return result.isEmpty() ? ItemStack.EMPTY : result.copy();
    }

    public static ItemStack craft(IInventory inventory, World world) {
        return inventory instanceof InventoryCrafting ? CraftingManager.findMatchingResult((InventoryCrafting) inventory, world) : ItemStack.EMPTY;
    }

    @Nullable
    public static IRecipe craftingRecipe(IInventory inventory, World world) {
        return inventory instanceof InventoryCrafting ? CraftingManager.findMatchingRecipe((InventoryCrafting) inventory, world) : null;
    }

    public static List<IRecipe> craftingRecipes() {
        return new ArrayList<>(ForgeRegistries.RECIPES.getValuesCollection());
    }

    private RecipeShims() {}
}
