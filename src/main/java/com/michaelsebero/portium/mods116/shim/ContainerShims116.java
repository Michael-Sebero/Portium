package com.michaelsebero.portium.mods116.shim;

import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;

import javax.annotation.Nullable;

/**
 * 1.16's container methods for the 1.16 glue, which can't name 1.12's classes (so some take objects), and 1.16's
 * {@code RecipeBookContainer} (crafting menus a recipe book fills) on 1.12's crafting containers, which have no such
 * base: the workbench's and the player's, through their crafting grids. Mods' own recipe book containers answer for
 * themselves.
 */
public final class ContainerShims116 {

    /** A 1.16 mod's container's menu type; 1.12's own containers have none. */
    public static Object type(Object container) {
        if (container instanceof com.michaelsebero.portium.mods116.adapter.Container116) return ((com.michaelsebero.portium.mods116.adapter.Container116) container).getType();
        throw new UnsupportedOperationException("Unable to construct this menu by type: " + container.getClass().getName() + " is 1.12's");
    }

    @Nullable
    private static InventoryCrafting grid(Container container) {
        if (container instanceof ContainerWorkbench) return ((ContainerWorkbench) container).craftMatrix;
        if (container instanceof ContainerPlayer) return ((ContainerPlayer) container).craftMatrix;
        return null;
    }

    @Nullable
    private static InventoryCraftResult result(Container container) {
        if (container instanceof ContainerWorkbench) return ((ContainerWorkbench) container).craftResult;
        if (container instanceof ContainerPlayer) return ((ContainerPlayer) container).craftResult;
        return null;
    }

    /** {@code fillCraftSlotsStackedContents(helper)}. */
    public static void fillStackedContents(Container container, RecipeItemHelper helper) {
        InventoryCrafting grid = grid(container);
        if (grid == null) return;
        for (int i = 0; i < grid.getSizeInventory(); i++) helper.accountStack(grid.getStackInSlot(i));
    }

    /** {@code clearCraftingContent()}. */
    public static void clearCraftingContent(Container container) {
        InventoryCrafting grid = grid(container);
        if (grid != null) grid.clear();
        InventoryCraftResult result = result(container);
        if (result != null) result.clear();
    }

    /** {@code recipeMatches(recipe)}. */
    public static boolean recipeMatches(Container container, IRecipe recipe) {
        InventoryCrafting grid = grid(container);
        return grid != null && recipe.matches(grid, null);
    }

    /** {@code getResultSlotIndex()}: 0 in both. */
    public static int resultSlot(Container container) {
        return 0;
    }

    public static int gridWidth(Container container) {
        InventoryCrafting grid = grid(container);
        return grid == null ? 0 : grid.getWidth();
    }

    public static int gridHeight(Container container) {
        InventoryCrafting grid = grid(container);
        return grid == null ? 0 : grid.getHeight();
    }

    /** {@code getSize()}: the grid and the result slot. */
    public static int size(Container container) {
        return gridWidth(container) * gridHeight(container) + 1;
    }

    private ContainerShims116() {}
}
