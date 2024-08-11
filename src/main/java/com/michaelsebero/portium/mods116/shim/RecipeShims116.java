package com.michaelsebero.portium.mods116.shim;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.12's crafting recipes asked as 1.16 recipes, for the 1.16 glue (which names 1.16's types, so these take objects):
 * 1.16 asks with any inventory, 1.12's recipes answer for crafting grids.
 */
public final class RecipeShims116 {

    @Nullable
    public static ResourceLocation registryName(Object recipe) {
        return ((IRecipe) recipe).getRegistryName();
    }

    public static boolean matches(Object recipe, Object inventory, Object world) {
        return inventory instanceof InventoryCrafting && ((IRecipe) recipe).matches((InventoryCrafting) inventory, (World) world);
    }

    public static Object craftingResult(Object recipe, Object inventory) {
        if (inventory instanceof InventoryCrafting) return ((IRecipe) recipe).getCraftingResult((InventoryCrafting) inventory);
        return ((IRecipe) recipe).getRecipeOutput().copy();
    }

    public static Object remainingItems(Object recipe, Object inventory) {
        if (inventory instanceof InventoryCrafting) return ((IRecipe) recipe).getRemainingItems((InventoryCrafting) inventory);
        return NonNullList.withSize(((IInventory) inventory).getSizeInventory(), ItemStack.EMPTY);
    }

    // 1.12's recipes for 1.16 mods' recipe manager: crafting as it is, furnace recipes made into 1.16's cooking kinds.

    /** 1.12's crafting recipes: vanilla's, 1.12 mods', and those Portium read from data packs (1.16 mods' included). */
    public static java.util.List<Object> legacyCrafting() {
        return new java.util.ArrayList<>(net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES.getValuesCollection());
    }

    /**
     * 1.12's furnace recipes, each {input, output, experience, blasts, smokes, name}: 1.16 also blasts what's
     * smelted from an ore or into nuggets (metal gear), and smokes and campfire-cooks what's smelted into food.
     */
    public static java.util.List<Object[]> legacySmelting() {
        java.util.List<Object[]> recipes = new java.util.ArrayList<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        net.minecraft.item.crafting.FurnaceRecipes furnace = net.minecraft.item.crafting.FurnaceRecipes.instance();
        for (java.util.Map.Entry<ItemStack, ItemStack> entry : furnace.getSmeltingList().entrySet()) {
            ItemStack input = entry.getKey();
            ItemStack output = entry.getValue();
            if (input.isEmpty() || output.isEmpty() || input.getItem().getRegistryName() == null) continue;
            boolean food = output.getItem() instanceof net.minecraft.item.ItemFood;
            boolean blasts = !food && (isOre(input) || output.getItem() == net.minecraft.init.Items.IRON_NUGGET
                    || output.getItem() == net.minecraft.init.Items.GOLD_NUGGET);
            ResourceLocation item = input.getItem().getRegistryName();
            int meta = input.getMetadata();
            String name = item.getNamespace() + "_" + item.getPath() + (meta == 0 || meta == net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE ? "" : "_" + meta);
            String unique = name;
            for (int n = 2; !names.add(unique); n++) unique = name + "_" + n;
            recipes.add(new Object[] {input.copy(), output.copy(), furnace.getSmeltingExperience(output), blasts, food, unique});
        }
        return recipes;
    }

    private static boolean isOre(ItemStack stack) {
        for (int id : net.minecraftforge.oredict.OreDictionary.getOreIDs(stack)) {
            if (net.minecraftforge.oredict.OreDictionary.getOreName(id).startsWith("ore")) return true;
        }
        return false;
    }

    private RecipeShims116() {}
}
