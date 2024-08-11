package com.michaelsebero.portium.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.mixin.accessor.FurnaceRecipesAccessor;
import com.michaelsebero.portium.modern.ModernMods;
import com.michaelsebero.portium.network.PortiumNetwork;
import com.michaelsebero.portium.network.RecipeSyncMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recipes from data packs (1.13). Crafting recipes are tried before the game's, and a pack recipe replaces the
 * game's recipe with the same id. Smelting recipes join the furnace's list until the next reload. Clients with
 * Portium receive the crafting recipes too, so the result they show matches the server's.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class DataPackRecipes {

    /** Replaced whole, since on an integrated server the client thread matches recipes while the server reloads. */
    private static volatile Crafting crafting = Crafting.EMPTY;
    private static final List<Smelting> addedSmelting = new ArrayList<>();
    private static final Map<ItemStack, ItemStack> replacedSmelting = new HashMap<>();

    /** The crafting recipes in effect, with the 1.12 JSON they were built from for sending to clients. */
    private static final class Crafting {

        static final Crafting EMPTY = new Crafting(Collections.emptyList(), Collections.emptySet(), Collections.emptyMap());

        final List<IRecipe> recipes;
        final Set<ResourceLocation> replaced;
        final Map<ResourceLocation, String> json;

        Crafting(List<IRecipe> recipes, Set<ResourceLocation> replaced, Map<ResourceLocation, String> json) {
            this.recipes = recipes;
            this.replaced = replaced;
            this.json = json;
        }
    }

    private static final class Smelting {

        final ItemStack input;
        final ItemStack result;

        Smelting(ItemStack input, ItemStack result) {
            this.input = input;
            this.result = result;
        }
    }

    static void load(DataPackManager packs, TagManager tags) {
        restoreSmelting();
        List<IRecipe> recipes = new ArrayList<>();
        Set<ResourceLocation> replaced = new HashSet<>();
        Map<ResourceLocation, String> json = new LinkedHashMap<>();
        for (ResourceLocation id : packs.list("recipes", ".json")) {
            // A pack's recipe file replaces the game's recipe with its id even if it fails to load, as in 1.13.
            replaced.add(id);
            try (InputStream stream = packs.open(id, "recipes", ".json")) {
                if (stream == null) continue;
                JsonObject recipe = DataPackJson.read(stream);
                // Forge 1.13 skips recipes whose conditions (mods loaded, items existing...) don't hold.
                if (!ModernMods.conditionsMet(recipe)) continue;
                String type = DataPackJson.bare(JsonUtils.getString(recipe, "type"));
                if (type.equals("crafting_shaped") || type.equals("crafting_shapeless")) {
                    JsonObject legacy = convertCrafting(recipe, type, tags);
                    recipes.add(parse(id, legacy));
                    json.put(id, legacy.toString());
                } else if (type.equals("smelting")) {
                    addSmelting(recipe, tags);
                }
                // The crafting_special_* types are recipes 1.12 already has built in.
            } catch (IOException | RuntimeException e) {
                DataPackJson.reportSkipped("recipe", id, e);
            }
        }
        crafting = new Crafting(Collections.unmodifiableList(recipes), Collections.unmodifiableSet(replaced), Collections.unmodifiableMap(json));
        PortiumNetwork.CHANNEL.sendToAll(new RecipeSyncMessage(crafting.json, crafting.replaced));
    }

    static void reset() {
        restoreSmelting();
        crafting = Crafting.EMPTY;
    }

    /** On a client, the recipes the server sent. */
    public static void receive(Map<ResourceLocation, String> json, Set<ResourceLocation> replaced) {
        List<IRecipe> recipes = new ArrayList<>();
        for (Map.Entry<ResourceLocation, String> entry : json.entrySet()) {
            try {
                recipes.add(parse(entry.getKey(), new JsonParser().parse(entry.getValue()).getAsJsonObject()));
            } catch (RuntimeException e) {
                Portium.LOGGER.error("Couldn't read the server's data pack recipe {}", entry.getKey(), e);
            }
        }
        crafting = new Crafting(Collections.unmodifiableList(recipes), Collections.unmodifiableSet(replaced), Collections.unmodifiableMap(json));
    }

    /** Clears recipes a server sent, when leaving it. */
    public static void forgetReceived() {
        crafting = Crafting.EMPTY;
    }

    @Nullable
    public static IRecipe findMatching(InventoryCrafting inventory, World world) {
        for (IRecipe recipe : crafting.recipes) {
            if (recipe.matches(inventory, world)) return recipe;
        }
        return null;
    }

    /** Whether a game recipe is replaced by a data pack's recipe of the same id. */
    public static boolean isReplaced(IRecipe recipe) {
        return !(recipe instanceof DataPackRecipe) && recipe.getRegistryName() != null && crafting.replaced.contains(recipe.getRegistryName());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) PortiumNetwork.CHANNEL.sendTo(new RecipeSyncMessage(crafting.json, crafting.replaced), (EntityPlayerMP) event.player);
    }

    // ---- Conversion from 1.13's format ----

    private static IRecipe parse(ResourceLocation id, JsonObject json) {
        return new DataPackRecipe(id, CraftingHelper.getRecipe(json, new JsonContext(id.getNamespace())));
    }

    private static JsonObject convertCrafting(JsonObject recipe, String type, TagManager tags) {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("type", "minecraft:" + type);
        if (recipe.has("group")) legacy.add("group", recipe.get("group"));
        if (type.equals("crafting_shaped")) {
            legacy.add("pattern", recipe.get("pattern"));
            JsonObject key = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : JsonUtils.getJsonObject(recipe, "key").entrySet()) key.add(entry.getKey(), convertIngredient(entry.getValue(), tags));
            legacy.add("key", key);
        } else {
            JsonArray ingredients = new JsonArray();
            for (JsonElement ingredient : JsonUtils.getJsonArray(recipe, "ingredients")) ingredients.add(convertIngredient(ingredient, tags));
            legacy.add("ingredients", ingredients);
        }
        legacy.add("result", convertResult(JsonUtils.getJsonObject(recipe, "result")));
        return legacy;
    }

    /** An item, a tag or a list of either, as a 1.12 ingredient: an item (any data value where 1.13 ignores it) or a list. */
    private static JsonElement convertIngredient(JsonElement ingredient, TagManager tags) {
        JsonArray items = new JsonArray();
        for (LegacyItem item : resolveIngredient(ingredient, tags)) {
            JsonObject json = new JsonObject();
            json.addProperty("item", DataPackJson.registryName(item.getItem()));
            boolean anyData = item.getMeta() == LegacyItem.ANY_META || !item.getItem().getHasSubtypes();
            json.addProperty("data", anyData ? OreDictionary.WILDCARD_VALUE : item.getMeta());
            if (item.getTag() != null) {
                json.addProperty("type", "minecraft:item_nbt");
                json.addProperty("nbt", item.getTag().toString());
            }
            items.add(json);
        }
        if (items.size() == 0) throw new JsonSyntaxException("Recipe ingredient matches no items: " + ingredient);
        return items.size() == 1 ? items.get(0) : items;
    }

    private static List<LegacyItem> resolveIngredient(JsonElement ingredient, TagManager tags) {
        List<LegacyItem> items = new ArrayList<>();
        if (ingredient.isJsonArray()) {
            for (JsonElement element : ingredient.getAsJsonArray()) items.addAll(resolveIngredient(element, tags));
            return items;
        }
        JsonObject object = ingredient.getAsJsonObject();
        if (object.has("tag")) {
            ResourceLocation id = new ResourceLocation(JsonUtils.getString(object, "tag"));
            Tag<LegacyItem> tag = tags.getItems().get(id);
            if (tag == null) throw new JsonSyntaxException("Unknown item tag '" + id + "'");
            items.addAll(tag.getValues());
        } else {
            String name = JsonUtils.getString(object, "item");
            LegacyItem item = LegacyItems.resolve(name);
            if (item == null) throw new JsonSyntaxException("Unknown item '" + name + "'");
            items.add(item);
        }
        return items;
    }

    private static JsonObject convertResult(JsonObject result) {
        String name = JsonUtils.getString(result, "item");
        LegacyItem item = LegacyItems.resolve(name);
        if (item == null) throw new JsonSyntaxException("Unknown item '" + name + "'");
        JsonObject legacy = new JsonObject();
        legacy.addProperty("item", DataPackJson.registryName(item.getItem()));
        legacy.addProperty("data", Math.max(item.getMeta(), 0));
        legacy.addProperty("count", JsonUtils.getInt(result, "count", 1));
        if (item.getTag() != null) legacy.addProperty("nbt", item.getTag().toString());
        return legacy;
    }

    // ---- Smelting ----

    /** 1.13 names the result as a plain item id; the experience and cooking time are optional. */
    private static void addSmelting(JsonObject recipe, TagManager tags) {
        JsonElement resultJson = recipe.get("result");
        String resultName = resultJson.isJsonObject() ? JsonUtils.getString(resultJson.getAsJsonObject(), "item") : resultJson.getAsString();
        LegacyItem resultItem = LegacyItems.resolve(resultName);
        if (resultItem == null) throw new JsonSyntaxException("Unknown item '" + resultName + "'");
        ItemStack result = resultItem.createStack(1);
        float experience = JsonUtils.getFloat(recipe, "experience", 0.0F);
        FurnaceRecipes furnace = FurnaceRecipes.instance();
        for (LegacyItem inputItem : resolveIngredient(recipe.get("ingredient"), tags)) {
            ItemStack input = new ItemStack(inputItem.getItem(), 1, inputItem.getMeta() == LegacyItem.ANY_META ? OreDictionary.WILDCARD_VALUE : inputItem.getMeta());
            ItemStack existing = findInput(furnace, input);
            if (existing != null) replacedSmelting.put(existing, furnace.getSmeltingList().remove(existing));
            ItemStack output = result.copy();
            furnace.getSmeltingList().put(input, output);
            ((FurnaceRecipesAccessor) furnace).portium$getExperienceList().put(output, experience);
            addedSmelting.add(new Smelting(input, output));
        }
    }

    @Nullable
    private static ItemStack findInput(FurnaceRecipes furnace, ItemStack input) {
        for (ItemStack key : furnace.getSmeltingList().keySet()) {
            if (key.getItem() == input.getItem() && (key.getMetadata() == OreDictionary.WILDCARD_VALUE || key.getMetadata() == input.getMetadata())) return key;
        }
        return null;
    }

    private static void restoreSmelting() {
        FurnaceRecipes furnace = FurnaceRecipes.instance();
        for (Smelting smelting : addedSmelting) {
            furnace.getSmeltingList().remove(smelting.input);
            ((FurnaceRecipesAccessor) furnace).portium$getExperienceList().remove(smelting.result);
        }
        addedSmelting.clear();
        furnace.getSmeltingList().putAll(replacedSmelting);
        replacedSmelting.clear();
    }

    private DataPackRecipes() {}
}
