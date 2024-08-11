package com.michaelsebero.portium.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Iterator;

/**
 * Portium's own loot tables without the entries for its items that the config switched off. 1.12 rejects a whole
 * table over one unknown item, which would empty buried treasure for want of a heart of the sea.
 */
public final class DisabledLoot {

    public static String prune(ResourceLocation table, String json) {
        if (!Tags.MOD_ID.equals(table.getNamespace())) return json;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (!root.has("pools") || !prunePools(root.getAsJsonArray("pools"))) return json;
            return root.toString();
        } catch (RuntimeException e) {
            // Leave anything unexpected to 1.12's own loading, and its error message.
            Portium.LOGGER.debug("Couldn't check loot table {} for switched-off items", table, e);
            return json;
        }
    }

    private static boolean prunePools(JsonArray pools) {
        boolean changed = false;
        for (JsonElement pool : pools) {
            if (!pool.isJsonObject() || !pool.getAsJsonObject().has("entries")) continue;
            Iterator<JsonElement> entries = pool.getAsJsonObject().getAsJsonArray("entries").iterator();
            while (entries.hasNext()) {
                JsonElement entry = entries.next();
                if (entry.isJsonObject() && isMissingItem(entry.getAsJsonObject())) {
                    entries.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean isMissingItem(JsonObject entry) {
        if (!entry.has("type") || !"item".equals(entry.get("type").getAsString()) || !entry.has("name")) return false;
        ResourceLocation item = new ResourceLocation(entry.get("name").getAsString());
        return Tags.MOD_ID.equals(item.getNamespace()) && !ForgeRegistries.ITEMS.containsKey(item);
    }

    private DisabledLoot() {}
}
