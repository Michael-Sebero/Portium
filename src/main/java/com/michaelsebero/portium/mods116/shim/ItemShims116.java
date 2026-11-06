package com.michaelsebero.portium.mods116.shim;

import net.minecraft.creativetab.CreativeTabs;

/** Forge 1.16's item methods that 1.12 has another way, for 1.16 code. */
public final class ItemShims116 {

    /** The index the next creative tab takes: 1.12's own count of them. */
    public static int groupCount() {
        return CreativeTabs.getNextID();
    }

    /** Forge 1.16's {@code BrewingRecipeRegistry.addRecipe(input, ingredient, output)}, by ingredients. */
    public static boolean addBrewing(net.minecraft.item.crafting.Ingredient input, net.minecraft.item.crafting.Ingredient ingredient, net.minecraft.item.ItemStack output) {
        return net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(new net.minecraftforge.common.brewing.IBrewingRecipe() {
            @Override
            public boolean isInput(net.minecraft.item.ItemStack stack) {
                return input.apply(stack);
            }

            @Override
            public boolean isIngredient(net.minecraft.item.ItemStack stack) {
                return ingredient.apply(stack);
            }

            @Override
            public net.minecraft.item.ItemStack getOutput(net.minecraft.item.ItemStack in, net.minecraft.item.ItemStack with) {
                return isInput(in) && isIngredient(with) ? output.copy() : net.minecraft.item.ItemStack.EMPTY;
            }
        });
    }

    public static net.minecraftforge.common.brewing.BrewingRecipe brewingRecipe(net.minecraft.item.crafting.Ingredient input,
            net.minecraft.item.crafting.Ingredient ingredient, net.minecraft.item.ItemStack output) {
        net.minecraft.item.ItemStack[] inputs = input.getMatchingStacks();
        net.minecraft.item.ItemStack[] ingredients = ingredient.getMatchingStacks();
        return new net.minecraftforge.common.brewing.BrewingRecipe(inputs.length > 0 ? inputs[0] : net.minecraft.item.ItemStack.EMPTY,
                ingredients.length > 0 ? ingredients[0] : net.minecraft.item.ItemStack.EMPTY, output);
    }

    /** An ingredient's first stack (1.12 Forge's brewing recipes take stacks). */
    public static net.minecraft.item.ItemStack firstStack(net.minecraft.item.crafting.Ingredient ingredient) {
        net.minecraft.item.ItemStack[] stacks = ingredient.getMatchingStacks();
        return stacks.length > 0 ? stacks[0] : net.minecraft.item.ItemStack.EMPTY;
    }

    /** Forge 1.16's {@code EnchantmentType.create(name, canEnchant)}. */
    @SuppressWarnings("unchecked")
    public static net.minecraft.enchantment.EnumEnchantmentType enchantmentType(String name, java.util.function.Predicate<?> canEnchant) {
        java.util.function.Predicate<net.minecraft.item.Item> test = (java.util.function.Predicate<net.minecraft.item.Item>) canEnchant;
        return net.minecraftforge.common.util.EnumHelper.addEnchantmentType(name, item -> item != null && test.test(item));
    }

    /** What an item feeds, as 1.14 says it: a 1.16 mod's item's food, or a 1.12 food item's own numbers; null if it isn't food. */
    @javax.annotation.Nullable
    public static com.michaelsebero.portium.modern.adapter.FoodValues foodValues(net.minecraft.item.Item item) {
        if (item instanceof com.michaelsebero.portium.modern.adapter.ModernItem) {
            Object food = ((com.michaelsebero.portium.modern.adapter.ModernItem) item).portium$getFood();
            if (food instanceof com.michaelsebero.portium.modern.adapter.FoodValues) return (com.michaelsebero.portium.modern.adapter.FoodValues) food;
        }
        if (item instanceof net.minecraft.item.ItemFood) {
            net.minecraft.item.ItemFood food = (net.minecraft.item.ItemFood) item;
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
            return new com.michaelsebero.portium.modern.adapter.FoodValues(food.getHealAmount(stack), food.getSaturationModifier(stack), food.isWolfsFavoriteMeat(),
                    false, false, java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }
        return null;
    }

    /** 1.14's {@code item.isFood()}: 1.12's food items, and 1.16 mods' items made with food. */
    public static boolean isFood(net.minecraft.item.Item item) {
        return item instanceof net.minecraft.item.ItemFood
                || item instanceof com.michaelsebero.portium.modern.adapter.ModernItem && ((com.michaelsebero.portium.modern.adapter.ModernItem) item).portium$getFood() != null;
    }

    // 1.13's stacks of an item or block, for constructor references (ItemStack::new): flattened ones are 1.12 variants.

    public static net.minecraft.item.ItemStack stack(com.michaelsebero.portium.modern.mc.util.IItemProvider item) {
        return stack(item, 1);
    }

    public static net.minecraft.item.ItemStack stack(com.michaelsebero.portium.modern.mc.util.IItemProvider item, int count) {
        return new net.minecraft.item.ItemStack(com.michaelsebero.portium.modern.flat.Flattening.stackItem(item), count,
                com.michaelsebero.portium.modern.flat.Flattening.stackMeta(item));
    }

    public static net.minecraft.item.ItemStack stack(com.michaelsebero.portium.modern.mc.util.IItemProvider item, int count, net.minecraft.nbt.NBTTagCompound nbt) {
        return new net.minecraft.item.ItemStack(com.michaelsebero.portium.modern.flat.Flattening.stackItem(item), count,
                com.michaelsebero.portium.modern.flat.Flattening.stackMeta(item), nbt);
    }

    private ItemShims116() {}

    /** Forge 1.16's {@code Rarity.create(name, color)}: Forge 1.12's enum extension, named as it's named. */
    public static net.minecraft.item.EnumRarity createRarity(String name, net.minecraft.util.text.TextFormatting color) {
        for (net.minecraft.item.EnumRarity rarity : net.minecraft.item.EnumRarity.values()) {
            if (rarity.name().equals(name)) return rarity;
        }
        return net.minecraftforge.common.util.EnumHelper.addRarity(name, color, name);
    }
}
