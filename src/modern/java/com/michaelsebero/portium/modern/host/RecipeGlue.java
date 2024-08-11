package com.michaelsebero.portium.modern.host;

import com.michaelsebero.portium.modern.shim.RecipeShims;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.RecipeType;
import net.minecraftforge.common.crafting.VanillaRecipeTypes;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * 1.13's {@code World.getRecipeManager()} and what Forge 1.13 asks of it: recipes are 1.12's (furnace recipes for
 * smelting, the crafting manager for crafting), so the manager's methods answer from those.
 */
public final class RecipeGlue {

    private static RecipeManager manager;
    private static boolean tried;

    /** A recipe manager to call methods on; they're answered here, whatever it holds. */
    @Nullable
    public static synchronized RecipeManager manager(World world) {
        if (!tried) {
            tried = true;
            try {
                manager = new RecipeManager();
            } catch (LinkageError | RuntimeException e) {
                manager = null;
            }
        }
        return manager;
    }

    public static ItemStack getResult(@Nullable RecipeManager manager, IInventory inventory, World world, RecipeType<?> type) {
        if (type == VanillaRecipeTypes.SMELTING) return RecipeShims.smelt(inventory.func_70301_a(0));
        if (type == VanillaRecipeTypes.CRAFTING) return RecipeShims.craft(inventory, world);
        return ItemStack.field_190927_a;
    }

    public static ItemStack getCraftingResult(@Nullable RecipeManager manager, IInventory inventory, World world) {
        return RecipeShims.craft(inventory, world);
    }

    @Nullable
    public static IRecipe getRecipe(@Nullable RecipeManager manager, IInventory inventory, World world, RecipeType<?> type) {
        return type == VanillaRecipeTypes.CRAFTING ? RecipeShims.craftingRecipe(inventory, world) : null;
    }

    @Nullable
    public static IRecipe getCraftingRecipe(@Nullable RecipeManager manager, IInventory inventory, World world) {
        return RecipeShims.craftingRecipe(inventory, world);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List getRecipes(@Nullable RecipeManager manager, RecipeType<?> type) {
        return type == VanillaRecipeTypes.CRAFTING ? (List) RecipeShims.craftingRecipes() : Collections.emptyList();
    }

    private RecipeGlue() {}
}
