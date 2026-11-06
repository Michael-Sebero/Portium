package com.michaelsebero.portium.recipe;

import com.cleanroommc.assetmover.AssetMoverAPI;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stonecutter's recipes (1.14): 1.16.5's own, which AssetMover extracts from its jar (Portium ships no Mojang
 * data; {@code stonecutting_1_16.txt} lists their names), and data packs' {@code minecraft:stonecutting} recipes.
 * Listed as 1.14 lists them: sorted by what they make.
 */
public final class StonecuttingRecipes {

    private static final String VERSION = "1.16.5";
    private static final String NAMES = "/assets/" + Tags.MOD_ID + "/stonecutting_1_16.txt";
    private static final File EXTRACTED = new File("assetmover/assets/" + Tags.MOD_ID + "/vanilla_data_116/recipes");

    public static final class Recipe {

        public final ResourceLocation id;
        private final List<ItemStack> inputs;
        private final ItemStack result;
        /** What 1.14 sorts by: the result's id (all stonecutter results are blocks). */
        final String sortKey;

        public Recipe(ResourceLocation id, List<ItemStack> inputs, ItemStack result, String sortKey) {
            this.id = id;
            this.inputs = inputs;
            this.result = result;
            this.sortKey = sortKey;
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

        public String getSortKey() {
            return this.sortKey;
        }
    }

    private static volatile Map<ResourceLocation, Recipe> builtIn;
    private static volatile Map<ResourceLocation, Recipe> fromPacks = Collections.emptyMap();
    private static volatile Set<ResourceLocation> replaced = Collections.emptySet();

    /** Asks AssetMover for 1.16.5's stonecutting recipes. Must run during mod construction. */
    public static void fetch() {
        if (!Loader.isModLoaded("assetmover")) return;
        Map<String, String> files = new LinkedHashMap<>();
        for (String name : names()) {
            files.put("data/minecraft/recipes/" + name + ".json", "assets/" + Tags.MOD_ID + "/vanilla_data_116/recipes/" + name + ".json");
        }
        AssetMoverAPI.fromMinecraft(VERSION, files);
    }

    private static List<String> names() {
        List<String> names = new ArrayList<>();
        try (InputStream list = StonecuttingRecipes.class.getResourceAsStream(NAMES)) {
            if (list == null) return names;
            for (String line : new String(IOUtils.toByteArray(list), StandardCharsets.UTF_8).split("\n")) {
                if (!line.trim().isEmpty()) names.add(line.trim());
            }
        } catch (IOException e) {
            Portium.LOGGER.warn("Couldn't read Portium's list of 1.16 stonecutting recipes", e);
        }
        return names;
    }

    /** Every recipe that cuts this stack, in 1.14's order. */
    public static List<Recipe> find(ItemStack stack) {
        List<Recipe> found = new ArrayList<>();
        if (stack.isEmpty()) return found;
        for (Recipe recipe : fromPacks.values()) if (recipe.matches(stack)) found.add(recipe);
        Set<ResourceLocation> gone = replaced;
        for (Recipe recipe : builtIn().values()) if (!gone.contains(recipe.id) && recipe.matches(stack)) found.add(recipe);
        found.sort((a, b) -> a.sortKey.compareTo(b.sortKey));
        return found;
    }

    public static boolean hasRecipe(ItemStack stack) {
        return !find(stack).isEmpty();
    }

    public static void setFromPacks(Map<ResourceLocation, Recipe> recipes, Set<ResourceLocation> replacedIds) {
        fromPacks = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
        replaced = Collections.unmodifiableSet(replacedIds);
    }

    /** A recipe from 1.16's JSON: one ingredient item (or a list), a result id and a count. Null if it names unknown items. */
    @Nullable
    public static Recipe parse(ResourceLocation id, JsonObject json, @Nullable List<ItemStack> resolvedInputs) {
        String resultName = JsonUtils.getString(json, "result");
        LegacyItem result = LegacyItems.resolve(resultName);
        if (result == null) return null;
        List<ItemStack> inputs = resolvedInputs;
        if (inputs == null) {
            inputs = new ArrayList<>();
            JsonElement ingredient = json.get("ingredient");
            for (JsonElement element : ingredient.isJsonArray() ? ingredient.getAsJsonArray() : java.util.Collections.singletonList(ingredient)) {
                LegacyItem item = LegacyItems.resolve(JsonUtils.getString(element.getAsJsonObject(), "item"));
                if (item != null) inputs.add(CookingRecipes.stack(item));
            }
        }
        if (inputs.isEmpty()) return null;
        return new Recipe(id, Collections.unmodifiableList(inputs), result.createStack(JsonUtils.getInt(json, "count", 1)),
                new ResourceLocation(resultName).getPath());
    }

    private static Map<ResourceLocation, Recipe> builtIn() {
        Map<ResourceLocation, Recipe> recipes = builtIn;
        if (recipes == null) {
            synchronized (StonecuttingRecipes.class) {
                if (builtIn == null) builtIn = Collections.unmodifiableMap(load());
                recipes = builtIn;
            }
        }
        return recipes;
    }

    private static Map<ResourceLocation, Recipe> load() {
        Map<ResourceLocation, Recipe> recipes = new LinkedHashMap<>();
        for (String name : names()) {
            File file = new File(EXTRACTED, name + ".json");
            if (!file.isFile()) continue;
            try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
                ResourceLocation id = new ResourceLocation("minecraft", name);
                Recipe recipe = parse(id, new JsonParser().parse(reader).getAsJsonObject(), null);
                if (recipe != null) recipes.put(id, recipe);
            } catch (IOException | RuntimeException e) {
                Portium.LOGGER.warn("Couldn't read 1.16's stonecutting recipe {}", name, e);
            }
        }
        if (recipes.isEmpty()) Portium.LOGGER.warn("No stonecutting recipes: AssetMover hasn't extracted 1.16.5's yet (they come with the next launch)");
        return recipes;
    }

    private StonecuttingRecipes() {}
}
