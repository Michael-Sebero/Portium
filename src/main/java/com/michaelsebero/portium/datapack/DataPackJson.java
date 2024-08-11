package com.michaelsebero.portium.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.nbt.SnbtParser;
import com.michaelsebero.portium.compat.LegacyBlock;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItemNbt;
import com.michaelsebero.portium.compat.LegacyItems;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Rewrites the 1.13 pieces shared by data pack JSON files (item and block ids, item NBT) into 1.12's form. */
final class DataPackJson {

    /**
     * Reports a data pack file that couldn't be loaded. A file naming something this game doesn't have (a 1.13
     * item, or one of Portium's that the config switched off) gets one line; anything else gets the whole error.
     */
    static void reportSkipped(String kind, ResourceLocation id, Exception e) {
        if (e instanceof JsonParseException && e.getMessage() != null) {
            Portium.LOGGER.warn("Skipping {} {} from a data pack: {}", kind, id, e.getMessage());
        } else {
            Portium.LOGGER.error("Couldn't load {} {} from a data pack", kind, id, e);
        }
    }

    static JsonObject read(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    static Iterable<JsonElement> array(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : Collections.emptyList();
    }

    /** {@code minecraft:item} becomes {@code item}: 1.12 only knows bare names in some places. */
    static String bare(String name) {
        return name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
    }

    static String registryName(Item item) {
        return String.valueOf(Item.REGISTRY.getNameForObject(item));
    }

    static String registryName(Block block) {
        return String.valueOf(Block.REGISTRY.getNameForObject(block));
    }

    /**
     * 1.13 item NBT for an item, as 1.12 stores it on that item. The stack it builds also gives the data value, which
     * 1.13 keeps in the tag as {@code Damage}.
     */
    static ItemStack applyNbt(LegacyItem item, String snbt) throws CommandSyntaxException {
        ItemStack stack = item.createStack(1);
        LegacyItemNbt.apply(stack, SnbtParser.parseCompound(snbt));
        return stack;
    }

    /**
     * An item predicate: {@code item} gains 1.12's {@code data}, NBT is converted, and a {@code tag} becomes
     * Portium's {@code portium:tag} predicate, which checks the data pack's item tag.
     */
    static void convertItemPredicate(JsonObject predicate) {
        if (predicate.has("tag")) {
            predicate.addProperty("type", "portium:tag");
            return;
        }
        if (!predicate.has("item")) return;
        LegacyItem item = LegacyItems.resolve(predicate.get("item").getAsString());
        if (item == null) return;
        predicate.addProperty("item", registryName(item.getItem()));
        if (item.getMeta() != LegacyItem.ANY_META && item.getItem().getHasSubtypes()) predicate.addProperty("data", item.getMeta());
        if (predicate.has("nbt")) {
            try {
                NBTTagCompound tag = applyNbt(item, predicate.get("nbt").getAsString()).getTagCompound();
                if (tag == null) predicate.remove("nbt");
                else predicate.addProperty("nbt", tag.toString());
            } catch (CommandSyntaxException e) {
                predicate.remove("nbt");
            }
        }
    }

    /** A block and optional state, as the {@code enter_block} and {@code placed_block} triggers take them. */
    static void convertBlockPredicate(JsonObject json) {
        if (!json.has("block")) return;
        Map<String, String> properties = new HashMap<>();
        if (json.has("state")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("state").entrySet()) properties.put(entry.getKey(), entry.getValue().getAsString());
        }
        String name = json.get("block").getAsString();
        LegacyBlock block = LegacyBlockStates.resolve(name);
        if (block == null) return;
        IBlockState state = LegacyBlockStates.convert(name, properties);
        JsonObject legacyState = new JsonObject();
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (properties.containsKey(property.getName()) || block.getVariant().containsKey(property)) {
                legacyState.addProperty(property.getName(), valueName(state, property));
            }
        }
        json.addProperty("block", registryName(block.getBlock()));
        if (legacyState.size() > 0) json.add("state", legacyState);
        else json.remove("state");
    }

    private static <T extends Comparable<T>> String valueName(IBlockState state, IProperty<T> property) {
        return property.getName(state.getValue(property));
    }

    private DataPackJson() {}
}
