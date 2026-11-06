package com.michaelsebero.portium.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.arguments.selector.EntityTypes;
import com.michaelsebero.portium.compat.LegacyBiomes;
import com.michaelsebero.portium.mixin.accessor.AdvancementManagerAccessor;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Advancements from data packs. Ids, biomes and item NBT in criteria are rewritten for 1.12, and a {@code function}
 * reward runs the data pack function (1.12's would look for one of its own functions instead).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class DataPackAdvancements {

    private static final Map<ResourceLocation, ResourceLocation> REWARD_FUNCTIONS = new HashMap<>();

    /** Adds every enabled pack's advancements to those 1.12 is about to build, replacing any with the same id. */
    public static void addTo(Map<ResourceLocation, Advancement.Builder> builders) {
        REWARD_FUNCTIONS.clear();
        DataPackManager packs = DataPackManager.get();
        if (packs == null) return;
        for (ResourceLocation id : packs.list("advancements", ".json")) {
            try (InputStream stream = packs.open(id, "advancements", ".json")) {
                if (stream == null) continue;
                JsonObject json = conditional(DataPackJson.read(stream));
                if (json == null) continue;
                convert(id, json);
                builders.put(id, JsonUtils.gsonDeserialize(AdvancementManagerAccessor.portium$getGson(), json.toString(), Advancement.Builder.class));
            } catch (IOException | RuntimeException e) {
                DataPackJson.reportSkipped("advancement", id, e);
            }
        }
    }

    /**
     * Forge 1.14+'s conditional advancement ({@code {"advancements": [{"conditions": [...], "advancement": {...}}]}}):
     * the first variant whose conditions hold, or null if none does. Any other advancement as it is.
     */
    @javax.annotation.Nullable
    private static JsonObject conditional(JsonObject json) {
        if (!json.has("advancements") || json.has("criteria") || !json.get("advancements").isJsonArray()) return json;
        for (com.google.gson.JsonElement element : json.getAsJsonArray("advancements")) {
            if (!element.isJsonObject()) continue;
            JsonObject variant = element.getAsJsonObject();
            if (!variant.has("advancement") || !variant.get("advancement").isJsonObject()) continue;
            if (com.michaelsebero.portium.mods116.Mods116.conditionsMet(variant)
                    && com.michaelsebero.portium.modern.ModernMods.conditionsMet(variant)) return variant.getAsJsonObject("advancement");
        }
        return null;
    }

    private static void convert(ResourceLocation id, JsonObject json) {
        if (json.has("criteria")) {
            for (Map.Entry<String, JsonElement> criterion : json.getAsJsonObject("criteria").entrySet()) {
                JsonObject object = criterion.getValue().getAsJsonObject();
                if (unlocksUnknownRecipe(object)) {
                    // 1.12 rejects the advancement over a recipe outside its registry, as data packs' are; this
                    // criterion can't be met then, and the advancement's others still can.
                    object.addProperty("trigger", "minecraft:impossible");
                    object.remove("conditions");
                    continue;
                }
                if (object.has("conditions") && object.get("conditions").isJsonObject()) {
                    JsonObject conditions = object.getAsJsonObject("conditions");
                    // 1.14 wraps a location trigger's place in "location" (beside "player"); 1.12 reads the conditions as the place.
                    String trigger = object.has("trigger") ? DataPackJson.bare(object.get("trigger").getAsString()) : "";
                    if ((trigger.equals("location") || trigger.equals("slept_in_bed")) && conditions.has("location") && conditions.get("location").isJsonObject()) {
                        conditions = conditions.getAsJsonObject("location");
                        object.add("conditions", conditions);
                    }
                    foldComposites(conditions);
                }
                if (object.has("conditions")) convertTree(object.get("conditions"));
            }
        }
        if (json.has("display") && json.getAsJsonObject("display").has("icon")) {
            JsonObject icon = json.getAsJsonObject("display").getAsJsonObject("icon");
            DataPackJson.convertItemPredicate(icon);
            icon.remove("nbt");
        }
        if (json.has("rewards")) {
            JsonObject rewards = json.getAsJsonObject("rewards");
            if (rewards.has("function")) {
                REWARD_FUNCTIONS.put(id, new ResourceLocation(rewards.get("function").getAsString()));
                rewards.remove("function");
            }
            // 1.12 rejects the whole advancement over a recipe it lacks, such as a data pack's own or a 1.13 name.
            if (rewards.has("recipes")) {
                JsonArray recipes = new JsonArray();
                for (JsonElement recipe : rewards.getAsJsonArray("recipes")) {
                    if (CraftingManager.getRecipe(new ResourceLocation(recipe.getAsString())) != null) recipes.add(recipe);
                }
                rewards.add("recipes", recipes);
            }
        }
    }

    /** Condition fields 1.16 gives as lists of loot conditions on an entity, where 1.12 wants one entity predicate. */
    private static final java.util.Set<String> ENTITY_FIELDS = new java.util.HashSet<>(java.util.Arrays.asList("entity", "player", "projectile",
            "shooter", "parent", "partner", "child", "zombie", "villager", "killer", "direct_entity", "source_entity", "lightning", "bystander"));

    /**
     * 1.16's composite entity conditions ({@code [{condition: entity_properties, predicate: {...}}, ...]}) become the
     * one predicate 1.12 reads: the predicates of its plain entity_properties terms merged. Inverted and other terms
     * have no 1.12 form and are left out, so such a criterion asks a little less than 1.16's.
     */
    private static void foldComposites(JsonObject conditions) {
        for (Map.Entry<String, JsonElement> entry : new java.util.ArrayList<>(conditions.entrySet())) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (key.equals("victims") && value.isJsonArray()) {
                JsonArray victims = new JsonArray();
                for (JsonElement victim : value.getAsJsonArray()) victims.add(victim.isJsonArray() ? fold(victim.getAsJsonArray()) : victim);
                conditions.add(key, victims);
            } else if (ENTITY_FIELDS.contains(key) && value.isJsonArray()) {
                conditions.add(key, fold(value.getAsJsonArray()));
            } else if (value.isJsonObject()) {
                foldComposites(value.getAsJsonObject());
            }
        }
    }

    private static JsonObject fold(JsonArray terms) {
        JsonObject merged = new JsonObject();
        for (JsonElement term : terms) {
            if (!term.isJsonObject()) continue;
            JsonObject condition = term.getAsJsonObject();
            if (!condition.has("condition") || !DataPackJson.bare(condition.get("condition").getAsString()).equals("entity_properties")) continue;
            if (condition.has("entity") && !"this".equals(condition.get("entity").getAsString())) continue;
            if (!condition.has("predicate") || !condition.get("predicate").isJsonObject()) continue;
            for (Map.Entry<String, JsonElement> property : condition.getAsJsonObject("predicate").entrySet()) {
                if (!merged.has(property.getKey())) merged.add(property.getKey(), property.getValue());
            }
        }
        return merged;
    }

    /** 1.16's structure names (lower case) as 1.12's generators and Portium's know them. */
    private static String structureName(String name) {
        switch (DataPackJson.bare(name)) {
            case "fortress": return "Fortress";
            case "bastion_remnant": return "Bastion_Remnant";
            case "village": return "Village";
            case "stronghold": return "Stronghold";
            case "monument": return "Monument";
            case "mansion": return "Mansion";
            case "mineshaft": return "Mineshaft";
            case "endcity": case "end_city": return "EndCity";
            case "pillager_outpost": return "Pillager_Outpost";
            case "ruined_portal": return "Ruined_Portal";
            case "desert_pyramid": return "Desert_Pyramid";
            case "jungle_pyramid": return "Jungle_Pyramid";
            case "igloo": return "Igloo";
            case "swamp_hut": return "Swamp_Hut";
            case "shipwreck": return "Shipwreck";
            case "ocean_ruin": return "Ocean_Ruin";
            case "buried_treasure": return "Buried_Treasure";
            default: return name;
        }
    }

    private static boolean unlocksUnknownRecipe(JsonObject criterion) {
        if (!criterion.has("trigger") || !DataPackJson.bare(criterion.get("trigger").getAsString()).equals("recipe_unlocked")) return false;
        JsonObject conditions = criterion.has("conditions") ? criterion.getAsJsonObject("conditions") : new JsonObject();
        return !conditions.has("recipe") || CraftingManager.getRecipe(new ResourceLocation(conditions.get("recipe").getAsString())) == null;
    }

    /**
     * Rewrites every predicate in a criterion's conditions. Shapes tell them apart: a string {@code item} or a
     * {@code tag} makes an item predicate, a {@code block} a block predicate and a string {@code type} an entity predicate.
     */
    private static void convertTree(JsonElement element) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) convertTree(child);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        boolean itemPredicate = isString(object, "item") || isString(object, "tag");
        if (itemPredicate) DataPackJson.convertItemPredicate(object);
        if (isString(object, "block")) DataPackJson.convertBlockPredicate(object);
        if (isString(object, "feature")) object.addProperty("feature", structureName(object.get("feature").getAsString()));
        // Entity type tags (#minecraft:raiders) have no 1.12 form: the rest of the predicate still has to hold.
        if (!itemPredicate && isString(object, "type") && object.get("type").getAsString().startsWith("#")) object.remove("type");
        if (!itemPredicate && isString(object, "type")) {
            ResourceLocation legacy = EntityTypes.toLegacy(new ResourceLocation(object.get("type").getAsString()));
            if (legacy != null) object.addProperty("type", legacy.toString());
        }
        if (isString(object, "biome")) object.addProperty("biome", LegacyBiomes.toLegacy(object.get("biome").getAsString()));
        // 1.13 names dimensions with a namespace; 1.12 knows them by bare name.
        for (String key : new String[] {"dimension", "from", "to"}) {
            if (isString(object, key) && object.get(key).getAsString().startsWith("minecraft:")) {
                object.addProperty(key, object.get(key).getAsString().substring("minecraft:".length()));
            }
        }
        if (isString(object, "potion")) object.addProperty("potion", registryId(ForgeRegistries.POTION_TYPES, object.get("potion").getAsString()));
        if (isString(object, "enchantment")) object.addProperty("enchantment", registryId(ForgeRegistries.ENCHANTMENTS, object.get("enchantment").getAsString()));
        if (object.has("effects") && object.get("effects").isJsonObject()) {
            JsonObject effects = new JsonObject();
            for (Map.Entry<String, JsonElement> effect : object.getAsJsonObject("effects").entrySet()) {
                effects.add(registryId(ForgeRegistries.POTIONS, effect.getKey()), effect.getValue());
            }
            object.add("effects", effects);
        }
        List<JsonElement> children = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) children.add(entry.getValue());
        for (JsonElement child : children) convertTree(child);
    }

    private static boolean isString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isString();
    }

    /** A 1.13 id, or Portium's entry of the same name for things 1.13 added. */
    private static String registryId(IForgeRegistry<?> registry, String id) {
        ResourceLocation modern = new ResourceLocation(id);
        if (registry.containsKey(modern) || !"minecraft".equals(modern.getNamespace())) return modern.toString();
        ResourceLocation portium = new ResourceLocation(Tags.MOD_ID, modern.getPath());
        return registry.containsKey(portium) ? portium.toString() : modern.toString();
    }

    /** Runs a completed advancement's function reward as the player, as 1.13 does: permission level 2, no feedback. */
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent event) {
        ResourceLocation function = REWARD_FUNCTIONS.get(event.getAdvancement().getId());
        DataPackManager packs = DataPackManager.get();
        if (function == null || packs == null || !(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        CommandFunction found = packs.getFunctions().get(function);
        if (found == null) {
            Portium.LOGGER.error("Advancement {} rewards the missing function {}", event.getAdvancement().getId(), function);
            return;
        }
        CommandSource source = CommandSource.of(event.getEntityPlayer()).withPermissionLevel(2).withFeedbackDisabled();
        packs.getFunctions().execute(found, source);
    }

    private DataPackAdvancements() {}
}
