package com.michaelsebero.portium.recipe;

import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.item.ItemSuspiciousStew;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.14's {@code SuspiciousStewRecipe}: a bowl, a red and a brown mushroom and a small flower. */
public class RecipeSuspiciousStew extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return !this.getCraftingResult(inventory).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        boolean bowl = false, red = false, brown = false;
        Object[] effect = null;
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item == Items.BOWL && !bowl) {
                bowl = true;
            } else if (item == Item.getItemFromBlock(Blocks.RED_MUSHROOM) && !red) {
                red = true;
            } else if (item == Item.getItemFromBlock(Blocks.BROWN_MUSHROOM) && !brown) {
                brown = true;
            } else if (effect == null && ItemSuspiciousStew.flowerEffect(stack) != null) {
                effect = ItemSuspiciousStew.flowerEffect(stack);
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (!bowl || !red || !brown || effect == null || PortiumItems.SUSPICIOUS_STEW == null) return ItemStack.EMPTY;
        ItemStack stew = new ItemStack(PortiumItems.SUSPICIOUS_STEW);
        ItemSuspiciousStew.addEffect(stew, (Potion) effect[0], (Integer) effect[1]);
        return stew;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return PortiumItems.SUSPICIOUS_STEW == null ? ItemStack.EMPTY : new ItemStack(PortiumItems.SUSPICIOUS_STEW);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
