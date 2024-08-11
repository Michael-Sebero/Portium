package com.michaelsebero.portium.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

import java.util.Locale;
import java.util.Random;

/**
 * 1.13's {@code exploration_map} loot function: turns an empty map into a map of the nearest structure of a kind,
 * marked with a decoration. 1.12 has no red X marker, so {@code red_x} draws the white one.
 */
public class ExplorationMap extends LootFunction {

    private final String destination;
    private final MapDecoration.Type decoration;
    private final byte zoom;
    private final boolean skipExistingChunks;

    public ExplorationMap(LootCondition[] conditions, String destination, MapDecoration.Type decoration, byte zoom, boolean skipExistingChunks) {
        super(conditions);
        this.destination = destination;
        this.decoration = decoration;
        this.zoom = zoom;
        this.skipExistingChunks = skipExistingChunks;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random random, LootContext context) {
        if (stack.getItem() != Items.MAP) return stack;
        BlockPos origin = LootOrigin.of(context);
        if (origin == null) return stack;
        World world = context.getWorld();
        BlockPos found = world.findNearestStructure(this.destination, origin, this.skipExistingChunks);
        if (found == null) return stack;
        ItemStack map = ItemMap.setupNewMap(world, found.getX(), found.getZ(), this.zoom, true, true);
        ItemMap.renderBiomePreviewMap(world, map);
        MapData.addTargetDecoration(map, found, "+", this.decoration);
        map.setTranslatableName("filled_map." + this.destination.toLowerCase(Locale.ROOT));
        return map;
    }

    private static MapDecoration.Type decoration(String name) {
        if ("red_x".equals(name)) return MapDecoration.Type.TARGET_X;
        for (MapDecoration.Type type : MapDecoration.Type.values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(name)) return type;
        }
        return MapDecoration.Type.MANSION;
    }

    public static class Serializer extends LootFunction.Serializer<ExplorationMap> {

        public Serializer() {
            super(new ResourceLocation("exploration_map"), ExplorationMap.class);
        }

        @Override
        public void serialize(JsonObject json, ExplorationMap function, JsonSerializationContext context) {
            json.addProperty("destination", function.destination);
            json.addProperty("decoration", function.decoration.name().toLowerCase(Locale.ROOT));
            json.addProperty("zoom", function.zoom);
            json.addProperty("skip_existing_chunks", function.skipExistingChunks);
        }

        @Override
        public ExplorationMap deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            return new ExplorationMap(conditions, JsonUtils.getString(json, "destination", "Buried_Treasure"),
                    decoration(JsonUtils.getString(json, "decoration", "mansion")), (byte) JsonUtils.getInt(json, "zoom", 2),
                    JsonUtils.getBoolean(json, "skip_existing_chunks", true));
        }
    }
}
