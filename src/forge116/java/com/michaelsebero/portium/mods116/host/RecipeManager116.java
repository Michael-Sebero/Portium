package com.michaelsebero.portium.mods116.host;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.BlastingRecipe;
import net.minecraft.item.crafting.CampfireCookingRecipe;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.item.crafting.SmokingRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 1.16's recipe manager for 1.16 mods: every recipe file in the game's data packs (the 1.16 mods' own among them),
 * read by its type's serializer from the recipe serializer registry where Forge's conditions hold, and found by
 * recipe type as 1.16 finds them. One for the game, read again whenever Portium reloads the data packs. Recipe types
 * 1.12 crafts itself (crafting, smelting) are 1.12's own as well; 1.16 code finds those it has serializers for.
 */
public class RecipeManager116 extends RecipeManager {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    private static RecipeManager116 game;

    private Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> byType = Collections.emptyMap();
    private Map<ResourceLocation, IRecipe<?>> byId = Collections.emptyMap();
    private boolean stale = true;

    /** The game's, read from the data packs when first needed after each reload. */
    public static synchronized RecipeManager116 game() {
        if (game == null) game = new RecipeManager116();
        if (game.stale) game.read();
        return game;
    }

    public static RecipeManager of(World world) {
        return game();
    }

    public static RecipeManager of(net.minecraft.world.server.ServerWorld world) {
        return game();
    }

    public static RecipeManager of(MinecraftServer server) {
        return game();
    }

    /** Portium reloaded the data packs. */
    static synchronized void invalidate() {
        if (game != null) game.stale = true;
    }

    private void read() {
        this.stale = false;
        Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> types = new HashMap<>();
        Map<ResourceLocation, IRecipe<?>> ids = new LinkedHashMap<>();
        int skipped = 0;
        for (Map.Entry<String, String> source : com.michaelsebero.portium.datapack.DataPackManager.recipeSources().entrySet()) {
            ResourceLocation id = new ResourceLocation(source.getKey());
            try {
                JsonObject json = new JsonParser().parse(source.getValue()).getAsJsonObject();
                if (!CraftingHelper.processConditions(json, "conditions")) continue;
                IRecipe<?> recipe = read(id, json);
                if (recipe == null) continue;
                types.computeIfAbsent(recipe.func_222127_g(), type -> new LinkedHashMap<>()).put(id, recipe);
                ids.put(id, recipe);
            } catch (RuntimeException | LinkageError e) {
                // The first few with their trace, so a missing piece shows; the rest are usually the same.
                if (skipped++ < 3) LOGGER.warn("Couldn't read 1.16 recipe {}", id, e);
                else LOGGER.warn("Couldn't read 1.16 recipe {}: {}", id, e.toString());
            }
        }
        int modern = ids.size();
        int legacy = addLegacy(types, ids);
        this.byType = types;
        this.byId = ids;
        LOGGER.info("Read {} recipes of {} types for 1.16 mods ({} couldn't be read), and {} of the game's own", modern, types.size(), skipped, legacy);
    }

    /**
     * The game's own recipes as 1.16's: 1.12's crafting recipes as they are (1.12's recipe interface is 1.16's here),
     * and each furnace recipe as 1.16's smelting recipe, plus blasting for ores and metal gear and smoking and
     * campfire cooking for food, as 1.16's vanilla recipes go. Returns how many were added.
     */
    private static int addLegacy(Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> types, Map<ResourceLocation, IRecipe<?>> ids) {
        int before = ids.size();
        for (Object each : com.michaelsebero.portium.mods116.shim.RecipeShims116.legacyCrafting()) {
            ResourceLocation id = com.michaelsebero.portium.mods116.shim.RecipeShims116.registryName(each);
            if (id != null && !ids.containsKey(id)) add(types, ids, IRecipeType.field_222149_a, id, (IRecipe<?>) each);
        }
        for (Object[] smelting : com.michaelsebero.portium.mods116.shim.RecipeShims116.legacySmelting()) {
            ItemStack input = (ItemStack) smelting[0];
            ItemStack output = (ItemStack) smelting[1];
            float experience = (Float) smelting[2];
            String name = (String) smelting[5];
            Ingredient ingredient = Ingredient.func_193369_a(input);
            ResourceLocation id = new ResourceLocation("portium", "smelting/" + name);
            add(types, ids, IRecipeType.field_222150_b, id, new FurnaceRecipe(id, "", ingredient, output, experience, 200));
            if ((Boolean) smelting[3]) {
                id = new ResourceLocation("portium", "blasting/" + name);
                add(types, ids, IRecipeType.field_222151_c, id, new BlastingRecipe(id, "", ingredient, output, experience, 100));
            }
            if ((Boolean) smelting[4]) {
                id = new ResourceLocation("portium", "smoking/" + name);
                add(types, ids, IRecipeType.field_222152_d, id, new SmokingRecipe(id, "", ingredient, output, experience, 100));
                id = new ResourceLocation("portium", "campfire_cooking/" + name);
                add(types, ids, IRecipeType.field_222153_e, id, new CampfireCookingRecipe(id, "", ingredient, output, experience, 600));
            }
        }
        return ids.size() - before;
    }

    private static void add(Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> types, Map<ResourceLocation, IRecipe<?>> ids, IRecipeType<?> type,
                            ResourceLocation id, IRecipe<?> recipe) {
        if (ids.containsKey(id)) return;
        types.computeIfAbsent(type, key -> new LinkedHashMap<>()).put(id, recipe);
        ids.put(id, recipe);
    }

    /** A recipe by its type's serializer, or null if no serializer of that type is registered (1.12 crafts it itself). */
    private static IRecipe<?> read(ResourceLocation id, JsonObject json) {
        String type = json.get("type").getAsString();
        // Vanilla's kinds are 1.12's to read (Portium loads them from the same packs); they're added after, as the game's.
        if (type.startsWith("minecraft:")) return null;
        IRecipeSerializer<?> serializer = (IRecipeSerializer<?>) ForgeRegistries.RECIPE_SERIALIZERS.getValue(new ResourceLocation(type));
        if (serializer == null) return null;
        return serializer.func_199425_a_(id, json);
    }

    /** {@code RecipeManager.deserializeRecipe(id, json)}. */
    public static IRecipe<?> deserialize(ResourceLocation id, JsonObject json) {
        IRecipe<?> recipe = read(id, json);
        if (recipe == null) throw new IllegalArgumentException("No serializer for recipe type " + json.get("type") + " of " + id);
        return recipe;
    }

    @SuppressWarnings("unchecked")
    private <C extends IInventory, T extends IRecipe<C>> Collection<T> recipes(IRecipeType<T> type) {
        Map<ResourceLocation, IRecipe<?>> recipes = this.byType.get(type);
        return recipes == null ? Collections.emptyList() : (Collection<T>) (Collection<?>) recipes.values();
    }

    /** Development aid: {@code -Dportium.dev.recipes=true} logs each different lookup's type, first item and answer. */
    private static final boolean LOG_LOOKUPS = Boolean.getBoolean("portium.dev.recipes");
    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** getRecipe(type, inventory, world): the first that matches. */
    @Override
    public <C extends IInventory, T extends IRecipe<C>> Optional<T> func_215371_a(IRecipeType<T> type, C inventory, World world) {
        Optional<T> found = Optional.empty();
        for (T recipe : this.<C, T>recipes(type)) {
            if (recipe.func_77569_a(inventory, world)) {
                found = Optional.of(recipe);
                break;
            }
        }
        if (LOG_LOOKUPS) {
            String lookup = type + " " + (inventory.func_70302_i_() > 0 ? inventory.func_70301_a(0) : "nothing") + " -> "
                    + found.map(recipe -> String.valueOf(recipe.func_199560_c())).orElse("none") + " (" + this.<C, T>recipes(type).size() + " of the type)";
            if (LOGGED.add(lookup)) LOGGER.info("Recipe lookup: {}", lookup);
        }
        return found;
    }

    /** getRecipesForType(type). */
    @Override
    public <C extends IInventory, T extends IRecipe<C>> List<T> func_241447_a_(IRecipeType<T> type) {
        return new ArrayList<>(this.<C, T>recipes(type));
    }

    /** getRecipes(type, inventory, world): all that match, by result name. */
    @Override
    public <C extends IInventory, T extends IRecipe<C>> List<T> func_215370_b(IRecipeType<T> type, C inventory, World world) {
        List<T> found = new ArrayList<>();
        for (T recipe : this.<C, T>recipes(type)) {
            if (recipe.func_77569_a(inventory, world)) found.add(recipe);
        }
        found.sort(Comparator.comparing(recipe -> recipe.func_77571_b().func_77977_a()));
        return found;
    }

    /** getRecipeNonNull(type, inventory, world): what the first match leaves, or the inventory's items. */
    @Override
    public <C extends IInventory, T extends IRecipe<C>> NonNullList<ItemStack> func_215369_c(IRecipeType<T> type, C inventory, World world) {
        Optional<T> recipe = func_215371_a(type, inventory, world);
        if (recipe.isPresent()) return recipe.get().func_179532_b(inventory);
        NonNullList<ItemStack> items = NonNullList.func_191197_a(inventory.func_70302_i_(), ItemStack.field_190927_a);
        for (int i = 0; i < items.size(); i++) items.set(i, inventory.func_70301_a(i));
        return items;
    }

    /** getRecipe(id). */
    @Override
    public Optional<? extends IRecipe<?>> func_215367_a(ResourceLocation id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    /** getRecipes(). */
    @Override
    public Collection<IRecipe<?>> func_199510_b() {
        return Collections.unmodifiableCollection(this.byId.values());
    }

    /** getKeys(). */
    @Override
    public Stream<ResourceLocation> func_215378_c() {
        return this.byId.keySet().stream();
    }

    /** deserializeRecipes(recipes): the client's from the server. */
    @Override
    public void func_223389_a(Iterable<IRecipe<?>> recipes) {
        Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> types = new HashMap<>();
        Map<ResourceLocation, IRecipe<?>> ids = new LinkedHashMap<>();
        for (IRecipe<?> recipe : recipes) {
            types.computeIfAbsent(recipe.func_222127_g(), type -> new LinkedHashMap<>()).put(recipe.func_199560_c(), recipe);
            ids.put(recipe.func_199560_c(), recipe);
        }
        this.byType = types;
        this.byId = ids;
        this.stale = false;
    }
}
