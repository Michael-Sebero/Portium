package com.michaelsebero.portium.compat;

import com.cleanroommc.assetmover.AssetMoverAPI;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.command.PortiumCommands;
import com.michaelsebero.portium.command.arguments.selector.EntityTypes;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraftforge.fml.common.Loader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 1.13's translations, from 1.13.2's language files that AssetMover extracts (Portium ships no Mojang text). Keys
 * 1.12 lacks are added: the messages of 1.13's commands, arguments and data packs. Where 1.12 already has a key for a
 * command Portium now runs in its 1.13 form, 1.13's wording wins, since the arguments differ. Items take 1.13's names
 * ("Oak Log", not "Oak Wood"), and so do entities.
 */
@SuppressWarnings("deprecation")
public final class ModernLang {

    private static final String VERSION = "1.13.2";
    private static final File DIR = new File("assetmover/assets/" + Tags.MOD_ID + "/lang_1_13");

    /** Must run during mod construction. */
    public static void fetch(Collection<String> languages) {
        if (!Loader.isModLoaded("assetmover")) return;
        Map<String, String> files = new LinkedHashMap<>();
        for (String language : languages) {
            String name = language.toLowerCase(Locale.ROOT) + ".json";
            files.put("assets/minecraft/lang/" + name, "assets/" + Tags.MOD_ID + "/lang_1_13/" + name);
        }
        AssetMoverAPI.fromMinecraft(VERSION, files);
    }

    /**
     * 1.13 block names that no item carries (wall variants, crops, technical blocks); their names must not replace
     * the name of the item they convert to.
     */
    private static final Pattern BLOCK_ONLY = Pattern.compile("air|cave_air|void_air|water|lava|fire|bubble_column|nether_portal|end_portal|end_gateway"
            + "|moving_piston|piston_head|redstone_wire|tripwire|cocoa|carrots|potatoes|beetroots|wheat|kelp_plant|tall_seagrass|frosted_ice"
            + "|melon_stem|pumpkin_stem|attached_.*_stem|potted_.*|wall_torch|redstone_wall_torch|.*_wall_sign|.*_wall_banner|.*_wall_head"
            + "|.*_wall_skull|.*_wall_fan");

    /** 1.13's entity names under 1.12's keys ({@code entity.ItemFrame.name}, which 1.12 never translated, becomes "Item Frame"). */
    private static Map<String, String> entityNames(Map<String, String> text) {
        Map<String, String> names = new HashMap<>();
        String prefix = "entity.minecraft.";
        for (Map.Entry<String, String> entry : text.entrySet()) {
            if (!entry.getKey().startsWith(prefix) || entry.getKey().indexOf('.', prefix.length()) >= 0) continue;
            ResourceLocation legacy = EntityTypes.toLegacy(new ResourceLocation("minecraft", entry.getKey().substring(prefix.length())));
            if (legacy == null || EntityTypes.PLAYER.equals(legacy)) continue;
            String name = EntityList.getTranslationName(legacy);
            if (name != null) names.put("entity." + name + ".name", entry.getValue());
        }
        return names;
    }

    /** 1.13 names Portium maps to a different 1.13 thing's 1.12 item, for want of an exact match (1.12 has no undyed shulker box). */
    private static final Set<String> LOSSY = new HashSet<>(Arrays.asList("shulker_box"));

    /** New name keys for things 1.12 names alike but 1.13 tells apart, each with the shared key it replaces. */
    private static final Map<String, String> SPLIT_KEYS = new LinkedHashMap<>();

    /**
     * Adds 1.13's text for {@code language}, falling back to American English, to the current language map, and
     * gives items their 1.13 names. Returns what was added, which clients also give their screens' translations.
     */
    public static Map<String, String> inject(String language) {
        String code = language.toLowerCase(Locale.ROOT);
        Map<String, String> local = read(code);
        Map<String, String> english = code.equals("en_us") ? local : read("en_us");
        Map<String, String> added = new LinkedHashMap<>();
        collect(local, added);
        collect(english, added);
        // Items are only renamed in a language 1.13's text exists for, so names never fall back to English.
        if (!local.isEmpty() && PortiumConfig.visuals.modernNames) {
            added.putAll(itemNames(local));
            added.putAll(entityNames(local));
        }
        // Split keys keep the shared 1.12 name where 1.13 has none.
        for (Map.Entry<String, String> split : SPLIT_KEYS.entrySet()) {
            if (!added.containsKey(split.getKey())) added.put(split.getKey(), added.getOrDefault(split.getValue(), I18n.translateToLocal(split.getValue())));
        }
        if (added.isEmpty()) return added;
        StringBuilder lang = new StringBuilder();
        for (Map.Entry<String, String> entry : added.entrySet()) {
            lang.append(entry.getKey()).append('=').append(entry.getValue().replace('\n', ' ')).append('\n');
        }
        LanguageMap.inject(new ByteArrayInputStream(lang.toString().getBytes(StandardCharsets.UTF_8)));
        return added;
    }

    private static Map<String, String> read(String language) {
        Map<String, String> text = new LinkedHashMap<>();
        File file = new File(DIR, language + ".json");
        if (!file.isFile()) return text;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, JsonElement> entry : new JsonParser().parse(reader).getAsJsonObject().entrySet()) {
                text.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (IOException | RuntimeException e) {
            Portium.LOGGER.error("Couldn't read 1.13's {} translations", language, e);
        }
        return text;
    }

    private static void collect(Map<String, String> text, Map<String, String> added) {
        for (Map.Entry<String, String> entry : text.entrySet()) {
            String key = entry.getKey();
            if (added.containsKey(key) || (I18n.canTranslate(key) && !overrides(key))) continue;
            added.put(key, entry.getValue());
        }
    }

    /**
     * 1.13's item names under the 1.12 keys of the items they convert to ({@code tile.log.oak.name} becomes
     * "Oak Log"). 1.13 item names come first, then block names, then names that only match a 1.12 registry name.
     * Keys two names of the same rank disagree on are left alone, as are items that build their own names (banners).
     */
    private static Map<String, String> itemNames(Map<String, String> text) {
        Map<String, String> names = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (boolean modern : new boolean[] {true, false}) {
            for (String prefix : new String[] {"item.minecraft.", "block.minecraft."}) {
                Map<String, String> found = new HashMap<>();
                for (Map.Entry<String, String> entry : text.entrySet()) {
                    if (!entry.getKey().startsWith(prefix)) continue;
                    String id = entry.getKey().substring(prefix.length());
                    if (id.contains(".") || LOSSY.contains(id) || (prefix.startsWith("block") && BLOCK_ONLY.matcher(id).matches())) continue;
                    if (LegacyItems.isModernName("minecraft:" + id) != modern) continue;
                    LegacyItem item = LegacyItems.resolve("minecraft:" + id);
                    // Items told apart only by NBT (spawn eggs) share one 1.12 name.
                    if (item == null || item.getTag() != null) continue;
                    ItemStack stack = item.createStack(1);
                    String key = stack.getTranslationKey() + ".name";
                    if (names.containsKey(key) || ambiguous.contains(key) || !stack.getDisplayName().equals(I18n.translateToLocal(key))) continue;
                    String previous = found.put(key, entry.getValue());
                    if (previous != null && !previous.equals(entry.getValue())) ambiguous.add(key);
                }
                found.keySet().removeAll(ambiguous);
                names.putAll(found);
            }
        }
        return names;
    }

    /**
     * Gives red and brown mushrooms (and their blocks), oak and stone buttons, and snow layers and blocks names of
     * their own, as in 1.13. The enchanted golden apple's key comes from its mixin. Must run once, at startup.
     */
    public static void splitSharedNames() {
        split(Blocks.BROWN_MUSHROOM, "brown_mushroom");
        split(Blocks.RED_MUSHROOM, "red_mushroom");
        split(Blocks.BROWN_MUSHROOM_BLOCK, "brown_mushroom_block");
        split(Blocks.RED_MUSHROOM_BLOCK, "red_mushroom_block");
        split(Blocks.WOODEN_BUTTON, "oak_button");
        split(Blocks.SNOW_LAYER, "snow");
        SPLIT_KEYS.put("item." + Tags.MOD_ID + ".enchanted_golden_apple.name", "item.appleGold.name");
    }

    private static void split(Block block, String name) {
        String shared = block.getTranslationKey() + ".name";
        block.setTranslationKey(Tags.MOD_ID + "." + name);
        SPLIT_KEYS.put(block.getTranslationKey() + ".name", shared);
    }

    /** {@code commands.<name>.*} keys of commands the 1.13 dispatcher handles. */
    private static boolean overrides(String key) {
        if (!PortiumConfig.mechanics.modernCommands || !key.startsWith("commands.")) return false;
        int end = key.indexOf('.', "commands.".length());
        return end > 0 && PortiumCommands.handles(key.substring("commands.".length(), end));
    }

    private ModernLang() {}
}
