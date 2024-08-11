package com.michaelsebero.portium.compat;

import com.michaelsebero.portium.command.arguments.selector.EntityTypes;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates 1.13 scoreboard criteria into 1.12 ones. 1.13 writes statistics as {@code minecraft.mined:minecraft.stone};
 * 1.12 as {@code stat.mineBlock.minecraft.stone}. Other criteria ({@code dummy}, {@code health}, team kills...) are
 * spelled the same.
 */
public final class LegacyCriteria {

    private static final Map<String, String> STAT_TYPES = new HashMap<>();
    private static final Map<String, String> CUSTOM = new HashMap<>();

    static {
        STAT_TYPES.put("mined", "stat.mineBlock.");
        STAT_TYPES.put("crafted", "stat.craftItem.");
        STAT_TYPES.put("used", "stat.useItem.");
        STAT_TYPES.put("broken", "stat.breakItem.");
        STAT_TYPES.put("picked_up", "stat.pickup.");
        STAT_TYPES.put("dropped", "stat.drop.");
        CUSTOM.put("eat_cake_slice", "cakeSlicesEaten");
        CUSTOM.put("fill_cauldron", "cauldronFilled");
        CUSTOM.put("use_cauldron", "cauldronUsed");
        CUSTOM.put("clean_armor", "armorCleaned");
        CUSTOM.put("clean_banner", "bannerCleaned");
        CUSTOM.put("clean_shulker_box", "shulkerBoxCleaned");
        CUSTOM.put("interact_with_brewingstand", "brewingstandInteraction");
        CUSTOM.put("interact_with_beacon", "beaconInteraction");
        CUSTOM.put("inspect_dropper", "dropperInspected");
        CUSTOM.put("inspect_hopper", "hopperInspected");
        CUSTOM.put("inspect_dispenser", "dispenserInspected");
        CUSTOM.put("play_noteblock", "noteblockPlayed");
        CUSTOM.put("tune_noteblock", "noteblockTuned");
        CUSTOM.put("pot_flower", "flowerPotted");
        CUSTOM.put("trigger_trapped_chest", "trappedChestTriggered");
        CUSTOM.put("open_enderchest", "enderchestOpened");
        CUSTOM.put("enchant_item", "itemEnchanted");
        CUSTOM.put("play_record", "recordPlayed");
        CUSTOM.put("interact_with_furnace", "furnaceInteraction");
        CUSTOM.put("interact_with_crafting_table", "craftingTableInteraction");
        CUSTOM.put("open_chest", "chestOpened");
        CUSTOM.put("open_shulker_box", "shulkerBoxOpened");
        CUSTOM.put("sleep_in_bed", "sleepInBed");
    }

    /** The 1.12 criteria for a 1.13 name, or null if there is none. */
    @Nullable
    public static IScoreCriteria convert(String name) {
        IScoreCriteria direct = IScoreCriteria.INSTANCES.get(name);
        if (direct != null) return direct;
        int colon = name.indexOf(':');
        if (colon < 0) return null;
        ResourceLocation type = new ResourceLocation(name.substring(0, colon).replace('.', ':'));
        ResourceLocation value = new ResourceLocation(name.substring(colon + 1).replace('.', ':'));
        String legacy = legacyStatName(type.getPath(), value);
        return legacy == null ? null : IScoreCriteria.INSTANCES.get(legacy);
    }

    @Nullable
    private static String legacyStatName(String type, ResourceLocation value) {
        if ("custom".equals(type)) {
            String path = value.getPath();
            return "stat." + CUSTOM.getOrDefault(path, camelCase(path));
        }
        if ("killed".equals(type) || "killed_by".equals(type)) {
            ResourceLocation entity = EntityTypes.toLegacy(value);
            if (entity == null) return null;
            String translation = EntityList.getTranslationName(entity);
            return ("killed".equals(type) ? "stat.killEntity." : "stat.entityKilledBy.") + translation;
        }
        String prefix = STAT_TYPES.get(type);
        if (prefix == null) return null;
        if ("mined".equals(type)) {
            LegacyBlock block = LegacyBlockStates.resolve(value.toString());
            if (block == null) return null;
            return prefix + Block.REGISTRY.getNameForObject(block.getBlock()).toString().replace(':', '.');
        }
        LegacyItem item = LegacyItems.resolve(value.toString());
        if (item == null) return null;
        return prefix + Item.REGISTRY.getNameForObject(item.getItem()).toString().replace(':', '.');
    }

    private static String camelCase(String snake) {
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                result.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return result.toString();
    }

    private LegacyCriteria() {}
}
