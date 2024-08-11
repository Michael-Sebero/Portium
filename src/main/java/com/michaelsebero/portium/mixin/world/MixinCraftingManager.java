package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.datapack.DataPackRecipes;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Data pack crafting recipes are tried first, and game recipes they replace never match (1.13). */
@Mixin(CraftingManager.class)
public abstract class MixinCraftingManager {

    @Inject(method = "findMatchingResult", at = @At("HEAD"), cancellable = true)
    private static void portium$dataPackResult(InventoryCrafting inventory, World world, CallbackInfoReturnable<ItemStack> cir) {
        IRecipe recipe = DataPackRecipes.findMatching(inventory, world);
        if (recipe != null) cir.setReturnValue(recipe.getCraftingResult(inventory));
    }

    @Inject(method = "findMatchingRecipe", at = @At("HEAD"), cancellable = true)
    private static void portium$dataPackRecipe(InventoryCrafting inventory, World world, CallbackInfoReturnable<IRecipe> cir) {
        IRecipe recipe = DataPackRecipes.findMatching(inventory, world);
        if (recipe != null) cir.setReturnValue(recipe);
    }

    @Inject(method = "getRemainingItems", at = @At("HEAD"), cancellable = true)
    private static void portium$dataPackRemainder(InventoryCrafting inventory, World world, CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        IRecipe recipe = DataPackRecipes.findMatching(inventory, world);
        if (recipe != null) cir.setReturnValue(recipe.getRemainingItems(inventory));
    }

    @Redirect(method = {"findMatchingResult", "findMatchingRecipe", "getRemainingItems"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/crafting/IRecipe;matches(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Z"))
    private static boolean portium$skipReplacedRecipes(IRecipe recipe, InventoryCrafting inventory, World world) {
        return !DataPackRecipes.isReplaced(recipe) && recipe.matches(inventory, world);
    }
}
