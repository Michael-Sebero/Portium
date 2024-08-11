package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.shim.RecipeShims116;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.16's recipe methods, called on 1.12's recipe interface, which is the one both have here: a 1.16 recipe's class
 * has them (1.12's interface doesn't declare them, so they're found on the class), and 1.12's crafting recipes answer
 * as 1.16's crafting recipes would. Each name is tried as SRG and, in a development environment, as 1.12's MCP name.
 */
public final class RecipeGlue {

    private static final Class<?>[] NONE = new Class<?>[0];
    private static final Class<?>[] INVENTORY = {IInventory.class};
    private static final Class<?>[] INVENTORY_WORLD = {IInventory.class, World.class};
    private static final Map<Class<?>, Map<String, Optional<Method>>> METHODS = new ConcurrentHashMap<>();

    public static IRecipeType<?> getType(IRecipe<?> recipe) {
        Method method = find(recipe, NONE, "func_222127_g");
        return method == null ? IRecipeType.field_222149_a : (IRecipeType<?>) call(method, recipe);
    }

    public static ResourceLocation getId(IRecipe<?> recipe) {
        Method method = find(recipe, NONE, "func_199560_c");
        return method == null ? RecipeShims116.registryName(recipe) : (ResourceLocation) call(method, recipe);
    }

    @Nullable
    public static IRecipeSerializer<?> getSerializer(IRecipe<?> recipe) {
        Method method = find(recipe, NONE, "func_199559_b");
        // 1.12's own recipes have no 1.16 serializer (1.16's crafting ones would read 1.16's recipe classes).
        return method == null ? null : (IRecipeSerializer<?>) call(method, recipe);
    }

    public static ItemStack getIcon(IRecipe<?> recipe) {
        Method method = find(recipe, NONE, "func_222128_h");
        return method == null ? new ItemStack(Blocks.field_150462_ai) : (ItemStack) call(method, recipe);
    }

    public static boolean matches(IRecipe<?> recipe, IInventory inventory, World world) {
        Method method = find(recipe, INVENTORY_WORLD, "func_77569_a", "matches");
        return method == null ? RecipeShims116.matches(recipe, inventory, world) : (Boolean) call(method, recipe, inventory, world);
    }

    public static ItemStack getCraftingResult(IRecipe<?> recipe, IInventory inventory) {
        Method method = find(recipe, INVENTORY, "func_77572_b", "getCraftingResult");
        return method == null ? (ItemStack) RecipeShims116.craftingResult(recipe, inventory) : (ItemStack) call(method, recipe, inventory);
    }

    @SuppressWarnings("unchecked")
    public static NonNullList<ItemStack> getRemainingItems(IRecipe<?> recipe, IInventory inventory) {
        Method method = find(recipe, INVENTORY, "func_179532_b", "getRemainingItems");
        return (NonNullList<ItemStack>) (method == null ? RecipeShims116.remainingItems(recipe, inventory) : call(method, recipe, inventory));
    }

    /** The recipe's public method by one of the names with exactly these parameters, or null if it has none. */
    @Nullable
    private static Method find(Object recipe, Class<?>[] parameters, String... names) {
        Map<String, Optional<Method>> methods = METHODS.computeIfAbsent(recipe.getClass(), type -> new ConcurrentHashMap<>());
        return methods.computeIfAbsent(names[0], key -> {
            for (String name : names) {
                try {
                    return Optional.of(recipe.getClass().getMethod(name, parameters));
                } catch (NoSuchMethodException e) {
                    // Try the next name.
                }
            }
            return Optional.empty();
        }).orElse(null);
    }

    private static Object call(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Couldn't call " + method, e);
        }
    }

    private RecipeGlue() {}
}
