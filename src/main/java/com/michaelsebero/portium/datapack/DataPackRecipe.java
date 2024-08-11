package com.michaelsebero.portium.datapack;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * A crafting recipe from a data pack. It stays out of the recipe registry, whose numeric ids clients share, and
 * reports itself as dynamic so 1.12's recipe book leaves it alone. It keeps its id itself: Forge's registry name
 * setter warns about ids outside the loading mod's namespace, which every data pack recipe would be.
 */
public class DataPackRecipe implements IRecipe {

    private final ResourceLocation id;
    private final IRecipe recipe;

    public DataPackRecipe(ResourceLocation id, IRecipe recipe) {
        this.id = id;
        this.recipe = recipe;
    }

    @Override
    public IRecipe setRegistryName(ResourceLocation name) {
        throw new UnsupportedOperationException("A data pack recipe's id is fixed");
    }

    @Override
    public ResourceLocation getRegistryName() {
        return this.id;
    }

    @Override
    public Class<IRecipe> getRegistryType() {
        return IRecipe.class;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return this.recipe.matches(inventory, world);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return this.recipe.getCraftingResult(inventory);
    }

    @Override
    public boolean canFit(int width, int height) {
        return this.recipe.canFit(width, height);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.recipe.getRecipeOutput();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        return this.recipe.getRemainingItems(inventory);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.recipe.getIngredients();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public String getGroup() {
        return this.recipe.getGroup();
    }
}
