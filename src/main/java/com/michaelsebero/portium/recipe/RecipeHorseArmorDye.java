package com.michaelsebero.portium.recipe;

import com.michaelsebero.portium.item.ItemLeatherHorseArmor;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.DyeUtils;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;

/** Dyeing leather horse armour (1.14's {@code ArmorDyeRecipe}, which takes any dyeable leather): the colours mix as leather armour's do. */
public class RecipeHorseArmorDye extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return !this.getCraftingResult(inventory).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack armor = ItemStack.EMPTY;
        List<EnumDyeColor> dyes = new ArrayList<>();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemLeatherHorseArmor) {
                if (!armor.isEmpty()) return ItemStack.EMPTY;
                armor = stack;
            } else if (DyeUtils.isDye(stack)) {
                dyes.add(DyeUtils.colorFromStack(stack).orElse(EnumDyeColor.WHITE));
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (armor.isEmpty() || dyes.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = armor.copy();
        result.setCount(1);
        int[] sum = new int[3];
        int brightness = 0;
        int count = 0;
        if (ItemLeatherHorseArmor.hasColor(armor)) {
            int color = ItemLeatherHorseArmor.getColor(armor);
            int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
            brightness += Math.max(r, Math.max(g, b));
            sum[0] += r;
            sum[1] += g;
            sum[2] += b;
            ++count;
        }
        for (EnumDyeColor dye : dyes) {
            float[] rgb = dye.getColorComponentValues();
            int r = (int) (rgb[0] * 255.0F), g = (int) (rgb[1] * 255.0F), b = (int) (rgb[2] * 255.0F);
            brightness += Math.max(r, Math.max(g, b));
            sum[0] += r;
            sum[1] += g;
            sum[2] += b;
            ++count;
        }
        int r = sum[0] / count, g = sum[1] / count, b = sum[2] / count;
        float average = (float) brightness / (float) count;
        float max = (float) Math.max(r, Math.max(g, b));
        r = (int) ((float) r * average / max);
        g = (int) ((float) g * average / max);
        b = (int) ((float) b * average / max);
        ItemLeatherHorseArmor.setColor(result, (r << 8) + g << 8 | b);
        return result;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
