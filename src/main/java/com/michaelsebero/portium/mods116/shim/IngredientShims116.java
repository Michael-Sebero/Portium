package com.michaelsebero.portium.mods116.shim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import com.michaelsebero.portium.mods116.adapter.Ingredient116;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 1.16's ingredient methods, on 1.12's ingredients: 1.16 keeps lists of items and tags and reads them when asked
 * ({@link Ingredient116}); what 1.16 reads from JSON or a packet, or makes from stacks, becomes one of those or a plain
 * 1.12 ingredient. Forge 1.16's own ingredient types in JSON ({@code forge:nbt}, {@code forge:compound}) are read too.
 */
public final class IngredientShims116 {

    // Making

    /** {@code new Ingredient(lists)}: 1.16's item lists ({@code SingleItemList}, {@code TagList}), read when asked. */
    public static Ingredient lists(Stream<?> lists) {
        Object[] all = lists.toArray();
        Supplier<Collection<?>> stacks = () -> {
            List<Object> found = new ArrayList<>();
            for (Object list : all) found.addAll(listStacks(list));
            return found;
        };
        Supplier<JsonElement> json = () -> {
            if (all.length == 1) return listJson(all[0]);
            JsonArray array = new JsonArray();
            for (Object list : all) array.add(listJson(list));
            return array;
        };
        return lazy(stacks, json, all.length == 0, null);
    }

    /** {@code fromItemListStream}: as {@link #lists}, but no lists at all is {@link Ingredient#EMPTY}. */
    public static Ingredient fromLists(Stream<?> lists) {
        Ingredient made = lists(lists);
        return hasNoMatchingItems(made) ? Ingredient.EMPTY : made;
    }

    public static Ingredient fromStacks(Stream<ItemStack> stacks) {
        return Ingredient.fromStacks(stacks.filter(stack -> !stack.isEmpty()).toArray(ItemStack[]::new));
    }

    public static Ingredient fromItems(IItemProvider[] items) {
        return fromStacks(Arrays.stream(items).map(ItemShims116::stack));
    }

    /** {@code fromTag} of a named tag: the item tag of that name in the running world's data packs. */
    public static Object fromItemTag(ResourceLocation id) {
        return lazy(() -> TagShims116.itemStacks(id), () -> tagJson(id), false, null);
    }

    /** {@code fromTag} of a tag with no name: its items, from the glue that can ask a 1.16 tag. */
    public static Object ofStacks(Supplier<? extends Collection<?>> stacks) {
        return lazy(stacks, () -> stacksJson(stacks.get()), false, null);
    }

    private static Ingredient lazy(Supplier<? extends Collection<?>> stacks, Supplier<? extends JsonElement> json, boolean noLists,
                                   @Nullable Predicate<ItemStack> matcher) {
        return Ingredient116.of(stacks, json, noLists, matcher);
    }

    /** Tags were reloaded: 1.16 ingredients read their lists again. */
    public static void reloaded() {
        Ingredient116.reloaded();
    }

    // Asking

    public static boolean test(Ingredient ingredient, @Nullable ItemStack stack) {
        return ingredient.apply(stack);
    }

    /** {@code hasNoMatchingItems}: 1.16's asks about its lists, not the stacks they give (a tag not yet loaded counts). */
    public static boolean hasNoMatchingItems(Ingredient ingredient) {
        if (ingredient instanceof Ingredient116) return ((Ingredient116) ingredient).portium$hasNoLists();
        return ingredient == Ingredient.EMPTY || ingredient.getMatchingStacks().length == 0;
    }

    // Packets: the stacks the ingredient gives now, as 1.16 sends them.

    public static void write(Ingredient ingredient, PacketBuffer buffer) {
        ItemStack[] stacks = ingredient.getMatchingStacks();
        buffer.writeVarInt(stacks.length);
        for (ItemStack stack : stacks) buffer.writeItemStack(stack);
    }

    public static Ingredient read(PacketBuffer buffer) {
        int count = buffer.readVarInt();
        ItemStack[] stacks = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            try {
                stacks[i] = buffer.readItemStack();
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't read an ingredient's stack", e);
            }
        }
        return fromStacks(Arrays.stream(stacks));
    }

    // JSON

    public static JsonElement serialize(Ingredient ingredient) {
        if (ingredient instanceof Ingredient116) return (JsonElement) ((Ingredient116) ingredient).portium$json();
        return stacksJson(Arrays.asList(ingredient.getMatchingStacks()));
    }

    /** {@code Ingredient.deserialize}, with Forge 1.16's ingredient types as Forge's {@code CraftingHelper} reads them. */
    public static Ingredient deserialize(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) throw new JsonSyntaxException("Item cannot be null");
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            if (array.size() == 0) throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
            List<Ingredient> parts = new ArrayList<>();
            for (JsonElement element : array) parts.add(deserialize(element));
            return union(parts, json);
        }
        if (!json.isJsonObject()) throw new JsonSyntaxException("Expected item to be object or array of objects");
        JsonObject object = json.getAsJsonObject();
        String type = object.has("type") ? JsonUtils.getString(object, "type") : "minecraft:item";
        switch (type) {
            case "minecraft:item":
                return itemList(object);
            case "forge:compound": {
                JsonArray children = JsonUtils.getJsonArray(object, "children");
                List<Ingredient> parts = new ArrayList<>();
                for (JsonElement child : children) parts.add(deserialize(child));
                return union(parts, json);
            }
            case "forge:nbt":
                return nbt(object);
            default:
                throw new JsonSyntaxException("Unknown ingredient type: " + type);
        }
    }

    // Shaped recipes' keys and recipes' results, which 1.16's recipe readers parse with its own ingredients.

    /** {@code ShapedRecipe.deserializeKey}: each one-character symbol's ingredient; a space is always empty. */
    public static java.util.Map<String, Ingredient> shapedKey(JsonObject json) {
        java.util.Map<String, Ingredient> key = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(entry.getKey())) throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            key.put(entry.getKey(), deserialize(entry.getValue()));
        }
        key.put(" ", Ingredient.EMPTY);
        return key;
    }

    /** {@code ShapedRecipe.patternFromJson}: rows up to the size Forge 1.16 lets mods raise, not 1.12's 3 by 3. */
    public static String[] pattern(JsonArray json) {
        int height = MiscShims116.craftingHeight();
        int width = MiscShims116.craftingWidth();
        String[] rows = new String[json.size()];
        if (rows.length > height) throw new JsonSyntaxException("Invalid pattern: too many rows, " + height + " is maximum");
        if (rows.length == 0) throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        for (int i = 0; i < rows.length; i++) {
            String row = JsonUtils.getString(json.get(i), "pattern[" + i + "]");
            if (row.length() > width) throw new JsonSyntaxException("Invalid pattern: too many columns, " + width + " is maximum");
            if (i > 0 && rows[0].length() != row.length()) throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            rows[i] = row;
        }
        return rows;
    }

    /** {@code ShapedRecipe.deserializeItem}, as Forge reads it: an item by 1.16 name, a count and an optional tag. */
    public static ItemStack resultStack(JsonObject json) {
        String name = JsonUtils.getString(json, "item");
        ItemStack stack = (ItemStack) TagShims116.itemStack(name);
        if (stack == null) throw new JsonSyntaxException("Unknown item '" + name + "'");
        if (json.has("data")) throw new JsonParseException("Disallowed data tag found");
        if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) stack.setItemDamage(0);
        if (json.has("nbt")) {
            JsonElement nbt = json.get("nbt");
            try {
                stack.setTagCompound(JsonToNBT.getTagFromJson(nbt.isJsonObject() ? nbt.toString() : JsonUtils.getString(nbt, "nbt")));
            } catch (NBTException e) {
                throw new JsonSyntaxException("Invalid NBT entry: " + e);
            }
        }
        stack.setCount(JsonUtils.getInt(json, "count", 1));
        return stack;
    }

    private static Ingredient itemList(JsonObject object) {
        if (object.has("item") && object.has("tag")) throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
        if (object.has("item")) {
            String name = JsonUtils.getString(object, "item");
            ItemStack stack = (ItemStack) TagShims116.itemStack(name);
            if (stack == null) throw new JsonSyntaxException("Unknown item '" + name + "'");
            JsonObject json = new JsonObject();
            json.addProperty("item", name);
            return lazy(() -> Collections.singletonList(stack), () -> json, false, null);
        }
        if (object.has("tag")) return (Ingredient) fromItemTag(new ResourceLocation(JsonUtils.getString(object, "tag")));
        throw new JsonParseException("An ingredient entry needs either a tag or an item");
    }

    /** Forge's NBT ingredient: one item with exactly that tag. */
    private static Ingredient nbt(JsonObject object) {
        String name = JsonUtils.getString(object, "item");
        ItemStack stack = (ItemStack) TagShims116.itemStack(name);
        if (stack == null) throw new JsonSyntaxException("Unknown item '" + name + "'");
        if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) stack.setItemDamage(0);
        NBTTagCompound tag = null;
        if (object.has("nbt")) {
            JsonElement nbt = object.get("nbt");
            try {
                tag = JsonToNBT.getTagFromJson(nbt.isJsonObject() ? nbt.toString() : JsonUtils.getString(nbt, "nbt"));
            } catch (NBTException e) {
                throw new JsonSyntaxException("Invalid NBT entry: " + e);
            }
        }
        stack.setTagCompound(tag);
        NBTTagCompound wanted = tag;
        return lazy(() -> Collections.singletonList(stack), () -> object, false,
                input -> input.getItem() == stack.getItem() && input.getMetadata() == stack.getMetadata()
                        && (wanted == null ? !input.hasTagCompound() || input.getTagCompound().isEmpty() : wanted.equals(input.getTagCompound())));
    }

    /** Several ingredients as one (an array, or Forge's compound): any of them. */
    private static Ingredient union(List<Ingredient> parts, JsonElement json) {
        if (parts.size() == 1) return parts.get(0);
        return lazy(() -> parts.stream().flatMap(part -> Arrays.stream(part.getMatchingStacks())).collect(Collectors.toList()), () -> json, false,
                input -> parts.stream().anyMatch(part -> part.apply(input)));
    }

    private static JsonElement tagJson(ResourceLocation id) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", id.toString());
        return json;
    }

    private static JsonElement stacksJson(Collection<?> stacks) {
        JsonArray array = new JsonArray();
        for (Object value : stacks) {
            if (!(value instanceof ItemStack) || ((ItemStack) value).isEmpty()) continue;
            JsonObject item = new JsonObject();
            ResourceLocation name = ((ItemStack) value).getItem().getRegistryName();
            item.addProperty("item", String.valueOf(name));
            array.add(item);
        }
        return array.size() == 1 ? array.get(0) : array;
    }

    // 1.16's item lists, which are Minecraft 1.16.5's own classes here: asked by name.

    private static Collection<?> listStacks(Object list) {
        Object stacks = call(list, "func_199799_a", "getStacks");
        return stacks instanceof Collection ? (Collection<?>) stacks : Collections.emptyList();
    }

    private static JsonElement listJson(Object list) {
        Object json = call(list, "func_200303_b", "serialize");
        return json instanceof JsonElement ? (JsonElement) json : new JsonObject();
    }

    @Nullable
    private static Object call(Object target, String... names) {
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException e) {
                // Try the next name.
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't ask " + target + " for " + name, e);
            }
        }
        throw new IllegalStateException(target.getClass().getName() + " has none of " + Arrays.toString(names));
    }

    private IngredientShims116() {}
}
