package com.michaelsebero.portium.recipe;

import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.14's cooking recipes besides smelting: campfire cooking, smoking (the smoker) and blasting (the blast furnace).
 * The built-in ones are 1.16.5's, under 1.16's recipe ids, so a data pack recipe with that id replaces them as in
 * 1.16; data packs add more ({@code minecraft:campfire_cooking}, {@code smoking}, {@code blasting}).
 */
public final class CookingRecipes {

    public enum Type {
        CAMPFIRE_COOKING("campfire_cooking"),
        SMOKING("smoking"),
        BLASTING("blasting");

        public final String id;

        Type(String id) {
            this.id = id;
        }

        @Nullable
        public static Type byId(String id) {
            for (Type type : values()) if (type.id.equals(id)) return type;
            return null;
        }
    }

    /** 1.16's default cooking time for these three types, when a recipe gives none. */
    public static final int DEFAULT_TIME = 100;

    public static final class Recipe {

        public final ResourceLocation id;
        public final Type type;
        private final List<ItemStack> inputs;
        private final ItemStack result;
        public final float experience;
        public final int cookingTime;

        public Recipe(ResourceLocation id, Type type, List<ItemStack> inputs, ItemStack result, float experience, int cookingTime) {
            this.id = id;
            this.type = type;
            this.inputs = inputs;
            this.result = result;
            this.experience = experience;
            this.cookingTime = cookingTime;
        }

        public boolean matches(ItemStack stack) {
            for (ItemStack input : this.inputs) {
                if (OreDictionary.itemMatches(input, stack, false)) return true;
            }
            return false;
        }

        public ItemStack getResult() {
            return this.result.copy();
        }

        /** What it's made from (for sending to clients). */
        public List<ItemStack> getInputs() {
            return this.inputs;
        }
    }

    /** 1.16.5's recipes, resolved to 1.12 items when first needed (Portium's items may be switched off). */
    private static volatile Map<ResourceLocation, Recipe> builtIn;
    private static volatile Map<ResourceLocation, Recipe> fromPacks = Collections.emptyMap();
    /** Built-in recipe ids that data packs replace (with any recipe, or a file that failed to load). */
    private static volatile Set<ResourceLocation> replaced = Collections.emptySet();

    @Nullable
    public static Recipe find(Type type, ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Recipe recipe : fromPacks.values()) {
            if (recipe.type == type && recipe.matches(stack)) return recipe;
        }
        Set<ResourceLocation> gone = replaced;
        for (Recipe recipe : builtIn().values()) {
            if (recipe.type == type && !gone.contains(recipe.id) && recipe.matches(stack)) return recipe;
        }
        return fromFurnace(type, stack);
    }

    /** The experience a cooked item gives, by the recipe of this type that makes it. */
    public static float experienceFor(Type type, ItemStack result) {
        for (Map<ResourceLocation, Recipe> recipes : java.util.Arrays.asList(fromPacks, builtIn())) {
            for (Recipe recipe : recipes.values()) {
                if (recipe.type == type && recipe.result.getItem() == result.getItem() && recipe.result.getMetadata() == result.getMetadata()) return recipe.experience;
            }
        }
        return net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingExperience(result);
    }

    /**
     * Other mods' furnace recipes, which know nothing of 1.14's types (switch: mechanics.modCookingRecipes): their food
     * cooks in smokers and on campfires, their ores smelt in blast furnaces. Minecraft's and Portium's own items only
     * use the recipes above, as in 1.14.
     */
    @Nullable
    private static Recipe fromFurnace(Type type, ItemStack stack) {
        if (!com.michaelsebero.portium.PortiumConfig.mechanics.modCookingRecipes) return null;
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null || "minecraft".equals(id.getNamespace()) || "portium".equals(id.getNamespace())) return null;
        ItemStack result = net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingResult(stack);
        if (result.isEmpty()) return null;
        boolean food = stack.getItem() instanceof net.minecraft.item.ItemFood && result.getItem() instanceof net.minecraft.item.ItemFood;
        boolean ore = false;
        for (int oreId : OreDictionary.getOreIDs(stack)) ore |= OreDictionary.getOreName(oreId).startsWith("ore");
        if (type == Type.BLASTING ? !ore : !food) return null;
        float experience = net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingExperience(result);
        int time = type == Type.CAMPFIRE_COOKING ? 600 : DEFAULT_TIME;
        ResourceLocation key = new ResourceLocation("portium", "furnace_" + type.id);
        return new Recipe(key, type, Collections.singletonList(stack.copy()), result.copy(), experience, time);
    }

    /** All recipes of a type in effect (for recipe viewers). */
    public static List<Recipe> all(Type type) {
        List<Recipe> recipes = new ArrayList<>();
        for (Recipe recipe : fromPacks.values()) if (recipe.type == type) recipes.add(recipe);
        for (Recipe recipe : builtIn().values()) if (recipe.type == type && !replaced.contains(recipe.id)) recipes.add(recipe);
        return recipes;
    }

    /** Data packs' recipes, replacing the last reload's. */
    public static void setFromPacks(Map<ResourceLocation, Recipe> recipes, Set<ResourceLocation> replacedIds) {
        fromPacks = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
        replaced = Collections.unmodifiableSet(replacedIds);
    }

    // ---- 1.16.5's own ----

    private static Map<ResourceLocation, Recipe> builtIn() {
        Map<ResourceLocation, Recipe> recipes = builtIn;
        if (recipes == null) {
            synchronized (CookingRecipes.class) {
                if (builtIn == null) builtIn = Collections.unmodifiableMap(vanilla());
                recipes = builtIn;
            }
        }
        return recipes;
    }

    private static final String[][] FOODS = {
            {"baked_potato", "potato", "0.35"}, {"cooked_beef", "beef", "0.35"}, {"cooked_chicken", "chicken", "0.35"}, {"cooked_cod", "cod", "0.35"},
            {"cooked_mutton", "mutton", "0.35"}, {"cooked_porkchop", "porkchop", "0.35"}, {"cooked_rabbit", "rabbit", "0.35"},
            {"cooked_salmon", "salmon", "0.35"}, {"dried_kelp", "kelp", "0.1"}};

    private static Map<ResourceLocation, Recipe> vanilla() {
        Map<ResourceLocation, Recipe> recipes = new LinkedHashMap<>();
        for (String[] food : FOODS) {
            add(recipes, food[0] + "_from_campfire_cooking", Type.CAMPFIRE_COOKING, food[0], Float.parseFloat(food[2]), 600, food[1]);
            add(recipes, food[0] + "_from_smoking", Type.SMOKING, food[0], Float.parseFloat(food[2]), 100, food[1]);
        }
        add(recipes, "coal_from_blasting", Type.BLASTING, "coal", 0.1F, 100, "coal_ore");
        add(recipes, "diamond_from_blasting", Type.BLASTING, "diamond", 1.0F, 100, "diamond_ore");
        add(recipes, "emerald_from_blasting", Type.BLASTING, "emerald", 1.0F, 100, "emerald_ore");
        add(recipes, "gold_ingot_from_blasting", Type.BLASTING, "gold_ingot", 1.0F, 100, "gold_ore", "nether_gold_ore");
        add(recipes, "gold_nugget_from_blasting", Type.BLASTING, "gold_nugget", 0.1F, 100, "golden_pickaxe", "golden_shovel", "golden_axe", "golden_hoe",
                "golden_sword", "golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots", "golden_horse_armor");
        add(recipes, "iron_ingot_from_blasting", Type.BLASTING, "iron_ingot", 0.7F, 100, "iron_ore");
        add(recipes, "iron_nugget_from_blasting", Type.BLASTING, "iron_nugget", 0.1F, 100, "iron_pickaxe", "iron_shovel", "iron_axe", "iron_hoe",
                "iron_sword", "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots", "iron_horse_armor", "chainmail_helmet",
                "chainmail_chestplate", "chainmail_leggings", "chainmail_boots");
        add(recipes, "lapis_from_blasting", Type.BLASTING, "lapis_lazuli", 0.2F, 100, "lapis_ore");
        add(recipes, "netherite_scrap_from_blasting", Type.BLASTING, "netherite_scrap", 2.0F, 100, "ancient_debris");
        add(recipes, "quartz_from_blasting", Type.BLASTING, "quartz", 0.2F, 100, "nether_quartz_ore");
        add(recipes, "redstone_from_blasting", Type.BLASTING, "redstone", 0.7F, 100, "redstone_ore");
        return recipes;
    }

    /** A recipe by 1.16 item names; left out if its result is missing (a Portium item switched off). */
    private static void add(Map<ResourceLocation, Recipe> recipes, String id, Type type, String result, float experience, int time, String... inputs) {
        LegacyItem resultItem = LegacyItems.resolve("minecraft:" + result);
        if (resultItem == null) return;
        List<ItemStack> stacks = new ArrayList<>();
        for (String input : inputs) {
            LegacyItem item = LegacyItems.resolve("minecraft:" + input);
            if (item != null) stacks.add(stack(item));
        }
        if (stacks.isEmpty()) return;
        ResourceLocation key = new ResourceLocation("minecraft", id);
        recipes.put(key, new Recipe(key, type, Collections.unmodifiableList(stacks), resultItem.createStack(1), experience, time));
    }

    /** An item to match: any data value where 1.16 has one item for what 1.12 keeps as data values of one. */
    public static ItemStack stack(LegacyItem item) {
        boolean anyData = item.getMeta() == LegacyItem.ANY_META;
        return new ItemStack(item.getItem(), 1, anyData ? OreDictionary.WILDCARD_VALUE : item.getMeta());
    }

    private CookingRecipes() {}
}
