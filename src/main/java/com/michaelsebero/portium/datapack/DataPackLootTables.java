package com.michaelsebero.portium.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.mixin.accessor.LootTableManagerAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Loot tables from data packs. 1.13's format is 1.12's apart from the flattened item ids, so entries get their 1.12
 * item and data value (and NBT in 1.12's form) before 1.12 reads the table.
 */
public final class DataPackLootTables {

    /** 1.12's names for loot tables 1.13 renamed, and the 1.13 names packs use. */
    private static final Map<String, String> RENAMED = new HashMap<>();

    static {
        RENAMED.put("minecraft:entities/evocation_illager", "minecraft:entities/evoker");
        RENAMED.put("minecraft:entities/vindication_illager", "minecraft:entities/vindicator");
        RENAMED.put("minecraft:entities/snowman", "minecraft:entities/snow_golem");
        RENAMED.put("minecraft:entities/sheep/silver", "minecraft:entities/sheep/light_gray");
    }

    /** The table from the topmost enabled pack that has one, or null to fall back to the world's and the game's (also when the pack's is broken). */
    @Nullable
    public static LootTable load(ResourceLocation id, LootTableManager manager) {
        DataPackManager packs = DataPackManager.get();
        if (packs == null) return null;
        String modern = RENAMED.get(id.toString());
        try (InputStream stream = packs.open(modern != null ? new ResourceLocation(modern) : id, "loot_tables", ".json")) {
            if (stream == null) return null;
            JsonObject table = DataPackJson.read(stream);
            for (JsonElement pool : DataPackJson.array(table, "pools")) {
                Iterator<JsonElement> entries = DataPackJson.array(pool.getAsJsonObject(), "entries").iterator();
                while (entries.hasNext()) {
                    // An item this game doesn't have (one of Portium's switched off, say) drops out, not the whole table.
                    if (!convertEntry(entries.next().getAsJsonObject())) entries.remove();
                }
            }
            return ForgeHooks.loadLootTable(LootTableManagerAccessor.portium$getGson(), id, table.toString(), true, manager);
        } catch (IOException | RuntimeException e) {
            // The game's own table stays in use rather than leaving the mob or chest without loot.
            Portium.LOGGER.error("Couldn't load loot table {} from a data pack", id, e);
            return null;
        }
    }

    /** Converts an entry to 1.12's names. Returns false for an item entry whose item doesn't exist here. */
    private static boolean convertEntry(JsonObject entry) {
        if (!entry.has("type")) return true;
        entry.addProperty("type", DataPackJson.bare(entry.get("type").getAsString()));
        convertFunctions(entry);
        if (!entry.has("name") || !"item".equals(entry.get("type").getAsString())) return true;
        LegacyItem item = LegacyItems.resolve(entry.get("name").getAsString());
        if (item == null) return false;
        entry.addProperty("name", DataPackJson.registryName(item.getItem()));
        JsonArray functions = new JsonArray();
        if (item.getMeta() > 0) functions.add(setData(item.getMeta()));
        if (item.getTag() != null) functions.add(setNbt(item.getTag().toString()));
        for (JsonElement element : DataPackJson.array(entry, "functions")) {
            JsonObject function = element.getAsJsonObject();
            if ("set_nbt".equals(DataPackJson.bare(function.get("function").getAsString())) && function.has("tag")) {
                try {
                    ItemStack stack = DataPackJson.applyNbt(item, function.get("tag").getAsString());
                    if (stack.getMetadata() != Math.max(item.getMeta(), 0)) functions.add(setData(stack.getMetadata()));
                    if (stack.getTagCompound() == null) continue;
                    function.addProperty("tag", stack.getTagCompound().toString());
                } catch (CommandSyntaxException e) {
                    Portium.LOGGER.warn("Ignoring set_nbt with bad NBT in a data pack loot table: {}", e.getMessage());
                    continue;
                }
            }
            functions.add(function);
        }
        entry.add("functions", functions);
        return true;
    }

    /** Functions Portium provides under its own name. */
    private static void convertFunctions(JsonObject holder) {
        for (JsonElement element : DataPackJson.array(holder, "functions")) {
            JsonObject function = element.getAsJsonObject();
            if ("set_name".equals(DataPackJson.bare(function.get("function").getAsString()))) function.addProperty("function", Tags.MOD_ID + ":set_name");
        }
    }

    private static JsonObject setData(int data) {
        JsonObject function = new JsonObject();
        function.addProperty("function", "set_data");
        function.addProperty("data", data);
        return function;
    }

    private static JsonObject setNbt(String tag) {
        JsonObject function = new JsonObject();
        function.addProperty("function", "set_nbt");
        function.addProperty("tag", tag);
        return function;
    }

    private DataPackLootTables() {}
}
