package com.michaelsebero.portium.client;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.cleanroommc.assetmover.AssetMoverAPI;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Portium ships no Mojang assets. At first launch AssetMover copies the textures, model templates and sounds below
 * out of the official 1.21.1 client into a local resource pack, renamed into Portium's namespace and 1.12's
 * {@code blocks/}/{@code items/} texture folders.
 * <p>
 * 1.21.1 is the last version before armour textures moved; every other path here is unchanged since 1.13-1.16.
 */
public final class PortiumAssets {

    private static final String VERSION = "1.21.1";
    /** 1.16's own blocks and sounds come from 1.16 itself. */
    private static final String VERSION_116 = "1.16.5";
    /** The 1.16.5 sound files Portium's sounds.json names, one path per line (written by tools/genres.py). */
    private static final String SOUNDS_116 = "/assets/" + Tags.MOD_ID + "/sounds_1_16.txt";
    private static final String[] CORALS = {"tube", "brain", "bubble", "fire", "horn"};
    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    public static void fetch() {
        if (!Loader.isModLoaded("assetmover")) {
            Portium.LOGGER.warn("AssetMover is not installed, so Portium's textures and sounds will be missing.");
            return;
        }
        // AssetMover downloads one file at a time, in the order asked, and stops waiting a minute in. Textures and models
        // come out of each version's client jar (one download each) and must be there when the game stitches them, so
        // they're asked for first; the hundreds of sound files follow, and any still downloading are picked up by the
        // next launch.
        Map<String, String> assets = new LinkedHashMap<>();
        addTextures(assets);
        addModels(assets);
        Portium.LOGGER.info("Requesting {} textures and models from Minecraft {} through AssetMover", assets.size(), VERSION);
        AssetMoverAPI.fromMinecraft(VERSION, assets);
        // The sea pickle and turtle egg templates name a default texture of their own. Portium's models replace it, but
        // 1.12 still loads every texture a parent model names, so those two also keep their vanilla 1.13 paths.
        Map<String, String> defaults = new LinkedHashMap<>();
        for (String path : new String[] {"block/sea_pickle", "block/turtle_egg"}) {
            defaults.put("assets/minecraft/textures/" + path + ".png", "assets/minecraft/textures/" + path + ".png");
        }
        AssetMoverAPI.fromMinecraft(VERSION, defaults);
        Map<String, String> textures116 = new LinkedHashMap<>();
        addNetherTextures(textures116);
        addLights(textures116);
        AssetMoverAPI.fromMinecraft(VERSION_116, textures116);
        Map<String, String> defaults116 = new LinkedHashMap<>();
        addTemplateDefaults116(defaults116);
        AssetMoverAPI.fromMinecraft(VERSION_116, defaults116);
        Map<String, String> sounds = new LinkedHashMap<>();
        addSounds(sounds);
        AssetMoverAPI.fromMinecraft(VERSION, sounds);
        Map<String, String> sounds116 = new LinkedHashMap<>();
        addSounds116(sounds116);
        AssetMoverAPI.fromMinecraft(VERSION_116, sounds116);
    }

    private static void addNetherTextures(Map<String, String> assets) {
        for (String kind : new String[] {"crimson", "warped"}) {
            texture(assets, "block/" + kind + "_nylium", "block/" + kind + "_nylium_side", "block/" + kind + "_stem_top",
                    "block/stripped_" + kind + "_stem", "block/stripped_" + kind + "_stem_top", "block/" + kind + "_fungus", "block/" + kind + "_roots");
            animatedTexture(assets, "block/" + kind + "_stem");
        }
        texture(assets, "block/warped_wart_block", "block/shroomlight", "block/nether_sprouts", "block/weeping_vines", "block/weeping_vines_plant",
                "block/twisting_vines", "block/twisting_vines_plant", "block/soul_soil", "block/basalt_top", "block/basalt_side",
                "block/polished_basalt_top", "block/polished_basalt_side", "block/blackstone", "block/blackstone_top", "block/gilded_blackstone",
                "block/nether_gold_ore", "block/ancient_debris_top", "block/ancient_debris_side", "block/crying_obsidian", "item/nether_sprouts");
        animatedTexture(assets, "block/soul_fire_0", "block/soul_fire_1");
        // Crimson and warped wood sets (1.16): planks, doors, trapdoors, signs (block, item, as drawn).
        for (String wood : new String[] {"crimson", "warped"}) {
            texture(assets, "block/" + wood + "_planks", "block/" + wood + "_door_top", "block/" + wood + "_door_bottom", "block/" + wood + "_trapdoor",
                    "item/" + wood + "_door", "item/" + wood + "_sign", "entity/signs/" + wood);
        }
        // Blackstone family and nether bricks (1.16).
        texture(assets, "block/polished_blackstone", "block/polished_blackstone_bricks", "block/cracked_polished_blackstone_bricks",
                "block/chiseled_polished_blackstone", "block/cracked_nether_bricks", "block/chiseled_nether_bricks", "block/quartz_bricks");
        // 1.14's signs of 1.12's other woods: item and as drawn.
        for (String wood : new String[] {"spruce", "birch", "jungle", "acacia", "dark_oak"}) texture(assets, "item/" + wood + "_sign", "entity/signs/" + wood);
        // Netherite (1.16): its items, block, the smithing table and its screen, and the armour as worn.
        for (String item : new String[] {"scrap", "ingot", "sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"}) {
            texture(assets, "item/netherite_" + item);
        }
        texture(assets, "block/netherite_block", "block/smithing_table_top", "block/smithing_table_bottom", "block/smithing_table_front",
                "block/smithing_table_side", "gui/container/smithing", "models/armor/netherite_layer_1", "models/armor/netherite_layer_2");
    }

    /**
     * Lanterns, chains, soul torches and campfires (1.14, 1.16), with the model templates Portium's own models build on.
     * The chain's and unlit campfire's models are whole models in 1.16, so they come in as templates under new names.
     */
    private static void addLights(Map<String, String> assets) {
        texture(assets, "block/chain", "block/soul_torch", "block/campfire_log", "item/lantern", "item/soul_lantern", "item/chain", "item/campfire",
                "item/soul_campfire", "particle/soul_fire_flame");
        animatedTexture(assets, "block/lantern", "block/soul_lantern", "block/campfire_fire", "block/campfire_log_lit", "block/soul_campfire_fire",
                "block/soul_campfire_log_lit");
        for (int i = 0; i < 12; i++) texture(assets, "particle/big_smoke_" + i);
        model(assets, "block/template_lantern", "block/template_hanging_lantern", "block/template_campfire");
        modelAs(assets, "block/chain", "block/template_chain");
        modelAs(assets, "block/campfire_off", "block/template_campfire_off");
        // Target block, lodestone, respawn anchor.
        texture(assets, "block/target_side", "block/target_top", "block/lodestone_side", "block/lodestone_top", "block/respawn_anchor_bottom",
                "block/respawn_anchor_top_off");
        for (int i = 0; i <= 4; i++) texture(assets, "block/respawn_anchor_side" + i);
        animatedTexture(assets, "block/respawn_anchor_top");
        // 1.14's plants and dyes, and pots for 1.16's roots.
        texture(assets, "block/cornflower", "block/lily_of_the_valley", "block/wither_rose", "block/crimson_roots_pot", "block/warped_roots_pot",
                "block/bamboo_stalk", "block/bamboo_small_leaves", "block/bamboo_large_leaves", "block/bamboo_singleleaf", "block/bamboo_stage0",
                "item/sweet_berries", "item/bamboo", "item/white_dye", "item/blue_dye", "item/brown_dye", "item/black_dye");
        for (int i = 0; i < 4; i++) texture(assets, "block/sweet_berry_bush_stage" + i);
        for (int age = 0; age <= 1; age++) {
            for (int n = 1; n <= 4; n++) modelAs(assets, "block/bamboo" + n + "_age" + age, "block/template_bamboo" + n + "_age" + age);
        }
        modelAs(assets, "block/bamboo_small_leaves", "block/template_bamboo_small_leaves");
        modelAs(assets, "block/bamboo_large_leaves", "block/template_bamboo_large_leaves");
        modelAs(assets, "block/potted_bamboo", "block/template_potted_bamboo");
        // 1.14's barrel, smoker, blast furnace, composter, fletching table, and their screens.
        texture(assets, "block/barrel_top", "block/barrel_top_open", "block/barrel_side", "block/barrel_bottom", "block/smoker_top", "block/smoker_bottom",
                "block/smoker_side", "block/smoker_front", "block/blast_furnace_top", "block/blast_furnace_side", "block/blast_furnace_front",
                "block/composter_top", "block/composter_bottom", "block/composter_side", "block/composter_compost", "block/composter_ready",
                "block/fletching_table_front", "block/fletching_table_side", "block/fletching_table_top", "gui/container/smoker",
                "gui/container/blast_furnace", "particle/glint");
        animatedTexture(assets, "block/smoker_front_on", "block/blast_furnace_front_on");
        modelAs(assets, "block/orientable_with_bottom", "block/template_orientable_with_bottom");
        modelAs(assets, "block/composter", "block/template_composter");
        for (int level = 1; level <= 7; level++) modelAs(assets, "block/composter_contents" + level, "block/template_composter_contents" + level);
        modelAs(assets, "block/composter_contents_ready", "block/template_composter_contents_ready");
        // Grindstone and stonecutter (1.14).
        texture(assets, "block/grindstone_pivot", "block/grindstone_round", "block/grindstone_side", "block/stonecutter_bottom", "block/stonecutter_top",
                "block/stonecutter_side", "gui/container/grindstone", "gui/container/stonecutter");
        animatedTexture(assets, "block/stonecutter_saw");
        modelAs(assets, "block/grindstone", "block/template_grindstone");
        modelAs(assets, "block/stonecutter", "block/template_stonecutter");
        // Loom, cartography table and banner patterns (1.14).
        texture(assets, "block/loom_front", "block/loom_side", "block/loom_top", "block/loom_bottom", "block/cartography_table_top",
                "block/cartography_table_side1", "block/cartography_table_side2", "block/cartography_table_side3", "gui/container/loom",
                "gui/container/cartography_table");
        for (String pattern : new String[] {"flower", "creeper", "skull", "mojang", "globe", "piglin"}) texture(assets, "item/" + pattern + "_banner_pattern");
        // Bell (1.14): the frame's models, and the bell body its renderer draws.
        texture(assets, "block/bell_bottom", "entity/bell/bell_body", "item/bell");
        for (String model : new String[] {"bell_floor", "bell_ceiling", "bell_wall", "bell_between_walls"}) modelAs(assets, "block/" + model, "block/template_" + model);
        // Lectern (1.14).
        texture(assets, "block/lectern_base", "block/lectern_front", "block/lectern_sides", "block/lectern_top");
        modelAs(assets, "block/lectern", "block/template_lectern");
        // The jigsaw block (1.14), on 1.16's cube_directional.
        texture(assets, "block/jigsaw_top", "block/jigsaw_bottom", "block/jigsaw_side", "block/jigsaw_lock");
        modelAs(assets, "block/cube_directional", "block/template_cube_directional");
        // Scaffolding (1.14).
        texture(assets, "block/scaffolding_top", "block/scaffolding_side", "block/scaffolding_bottom");
        modelAs(assets, "block/scaffolding_stable", "block/template_scaffolding_stable");
        modelAs(assets, "block/scaffolding_unstable", "block/template_scaffolding_unstable");
        // Crossbow (1.14): its textures and 1.16.5's item model, for the display transforms.
        texture(assets, "item/crossbow_standby", "item/crossbow_pulling_0", "item/crossbow_pulling_1", "item/crossbow_pulling_2", "item/crossbow_arrow",
                "item/crossbow_firework");
        modelAs(assets, "item/crossbow", "item/template_crossbow");
        // Foxes (1.14).
        texture(assets, "entity/fox/fox", "entity/fox/fox_sleep", "entity/fox/snow_fox", "entity/fox/snow_fox_sleep");
        // Pandas (1.14).
        for (String panda : new String[] {"panda", "lazy_panda", "worried_panda", "playful_panda", "brown_panda", "weak_panda", "aggressive_panda"}) {
            texture(assets, "entity/panda/" + panda);
        }
        // Cats (1.14).
        for (String cat : new String[] {"tabby", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "white", "jellie",
                "all_black", "cat_collar"}) {
            texture(assets, "entity/cat/" + cat);
        }
        // Pillagers (1.14).
        texture(assets, "entity/illager/pillager");
        // Ravagers (1.14).
        texture(assets, "entity/illager/ravager");
        // Wandering traders (1.14) and the 1.14 trade screen.
        texture(assets, "entity/wandering_trader", "entity/llama/decor/trader_llama", "gui/container/villager2");
        // Villagers (1.14): the base skin, biome types, professions and level badges, and zombie villagers likewise.
        texture(assets, "entity/villager/villager", "entity/zombie_villager/zombie_villager");
        for (String kind : new String[] {"villager", "zombie_villager"}) {
            for (String type : new String[] {"desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"}) texture(assets, "entity/" + kind + "/type/" + type);
            for (String profession : new String[] {"armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher", "leatherworker",
                    "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith"}) {
                texture(assets, "entity/" + kind + "/profession/" + profession);
            }
            for (String level : new String[] {"stone", "iron", "gold", "emerald", "diamond"}) texture(assets, "entity/" + kind + "/profession_level/" + level);
        }
        // Piglins, hoglins and zoglins (1.16).
        texture(assets, "entity/piglin/piglin", "entity/piglin/piglin_brute", "entity/piglin/zombified_piglin", "entity/hoglin/hoglin", "entity/hoglin/zoglin",
                "entity/strider/strider", "entity/strider/strider_cold", "entity/strider/strider_saddle", "item/warped_fungus_on_a_stick");
        // Leather horse armour and suspicious stew (1.14).
        texture(assets, "item/leather_horse_armor", "entity/horse/armor/horse_armor_leather", "item/suspicious_stew", "entity/cow/brown_mooshroom");
        // Horses (1.15's model and 64x64 textures): coats, markings, armour, donkeys, mules, zombie and skeleton horses.
        for (String coat : new String[] {"white", "creamy", "chestnut", "brown", "black", "gray", "darkbrown", "zombie", "skeleton"}) {
            texture(assets, "entity/horse/horse_" + coat);
        }
        for (String marking : new String[] {"white", "whitefield", "whitedots", "blackdots"}) texture(assets, "entity/horse/horse_markings_" + marking);
        texture(assets, "entity/horse/donkey", "entity/horse/mule", "entity/horse/armor/horse_armor_iron", "entity/horse/armor/horse_armor_gold",
                "entity/horse/armor/horse_armor_diamond");
        // 1.13's map icon sheet (16 to a row: banner markers, the red X).
        texture(assets, "map/map_icons");
        // The Pigstep disc (1.16).
        texture(assets, "item/music_disc_pigstep");
        // Iron golem cracks (1.15).
        texture(assets, "entity/iron_golem/iron_golem_crackiness_low", "entity/iron_golem/iron_golem_crackiness_medium",
                "entity/iron_golem/iron_golem_crackiness_high");
        // Bees, nests and hives (1.15).
        texture(assets, "entity/bee/bee_stinger", "entity/bee/bee", "entity/bee/bee_nectar", "entity/bee/bee_angry", "entity/bee/bee_angry_nectar", "block/bee_nest_top",
                "block/bee_nest_bottom", "block/bee_nest_side", "block/bee_nest_front", "block/bee_nest_front_honey", "block/beehive_end",
                "block/beehive_side", "block/beehive_front", "block/beehive_front_honey");
        // Soul speed's soul particle (1.16).
        for (int frame = 0; frame <= 10; ++frame) texture(assets, "particle/soul_" + frame);
        // Honey and honeycomb (1.15).
        texture(assets, "block/honey_block_top", "block/honey_block_bottom", "block/honey_block_side", "block/honeycomb_block", "item/honey_bottle",
                "item/honeycomb");
        modelAs(assets, "block/honey_block", "block/template_honey_block");
    }

    /**
     * Textures the 1.16 templates name as defaults, which 1.12 loads even though Portium's models replace them. Asked
     * for apart from {@link #addLights}: AssetMover keys its requests by source file, and these are fetched twice.
     */
    private static void addTemplateDefaults116(Map<String, String> assets) {
        for (String path : new String[] {"block/chain", "block/campfire_log", "block/bamboo_stalk", "block/bamboo_small_leaves", "block/bamboo_large_leaves",
                "block/bamboo_singleleaf", "block/flower_pot", "block/dirt", "block/composter_top", "block/composter_bottom", "block/composter_side",
                "block/composter_compost", "block/composter_ready", "block/grindstone_pivot", "block/grindstone_round", "block/grindstone_side",
                "block/dark_oak_log", "block/dark_oak_planks", "block/stone", "block/bell_bottom", "block/oak_planks", "block/lectern_base", "block/lectern_front",
                "block/lectern_sides", "block/lectern_top", "block/scaffolding_top", "block/scaffolding_side", "block/scaffolding_bottom",
                "block/honey_block_top", "block/honey_block_bottom", "block/honey_block_side", "block/stonecutter_bottom", "block/stonecutter_top", "block/stonecutter_side", "block/stonecutter_saw",
                "item/crossbow_standby",
                // 1.16's globe and snout banner patterns, where 1.12's banner and shield renderers look for patterns.
                "entity/banner/globe", "entity/banner/piglin", "entity/shield/globe", "entity/shield/piglin"}) {
            assets.put("assets/minecraft/textures/" + path + ".png", "assets/minecraft/textures/" + path + ".png");
        }
    }

    /** The 1.16.5 sound files listed beside Portium's sounds.json. */
    private static void addSounds116(Map<String, String> assets) {
        try (InputStream list = PortiumAssets.class.getResourceAsStream(SOUNDS_116)) {
            if (list == null) {
                Portium.LOGGER.warn("Portium's list of 1.16 sounds is missing");
                return;
            }
            for (String line : new String(IOUtils.toByteArray(list), StandardCharsets.UTF_8).split("\n")) {
                if (!line.trim().isEmpty()) sound(assets, line.trim());
            }
        } catch (IOException e) {
            Portium.LOGGER.warn("Couldn't read Portium's list of 1.16 sounds", e);
        }
    }

    private static void addTextures(Map<String, String> assets) {
        texture(assets, "block/blue_ice");
        for (String wood : new String[] {"spruce", "birch", "jungle", "acacia", "dark_oak"}) texture(assets, "block/" + wood + "_trapdoor");
        for (String wood : WOODS) {
            texture(assets, "block/stripped_" + wood + "_log");
            texture(assets, "block/stripped_" + wood + "_log_top");
        }
        for (String coral : CORALS) {
            for (String prefix : new String[] {"", "dead_"}) {
                texture(assets, "block/" + prefix + coral + "_coral");
                texture(assets, "block/" + prefix + coral + "_coral_block");
                texture(assets, "block/" + prefix + coral + "_coral_fan");
            }
        }
        texture(assets, "block/dried_kelp_top", "block/dried_kelp_side", "block/dried_kelp_bottom");
        animatedTexture(assets, "block/kelp", "block/kelp_plant", "block/seagrass", "block/tall_seagrass_bottom", "block/tall_seagrass_top");
        animatedTexture(assets, "block/water_still", "block/water_flow");
        texture(assets, "block/sea_pickle", "block/conduit");
        texture(assets, "block/turtle_egg", "block/turtle_egg_slightly_cracked", "block/turtle_egg_very_cracked");

        texture(assets, "item/kelp", "item/seagrass", "item/sea_pickle", "item/turtle_egg", "item/dried_kelp", "item/heart_of_the_sea",
                "item/nautilus_shell", "item/phantom_membrane", "item/turtle_scute", "item/trident", "item/turtle_helmet");

        texture(assets, "entity/conduit/base", "entity/conduit/cage", "entity/conduit/closed_eye", "entity/conduit/open_eye",
                "entity/conduit/wind", "entity/conduit/wind_vertical", "entity/trident", "entity/trident_riptide", "models/armor/turtle_layer_1");
        texture(assets, "particle/nautilus", "particle/bubble_pop_0", "particle/bubble_pop_1", "particle/bubble_pop_2", "particle/bubble_pop_3", "particle/bubble_pop_4");
        texture(assets, "mob_effect/conduit_power", "mob_effect/dolphins_grace", "mob_effect/slow_falling");
        texture(assets, "mob_effect/bad_omen", "mob_effect/hero_of_the_village");

        texture(assets, "item/cod_bucket", "item/salmon_bucket", "item/pufferfish_bucket", "item/tropical_fish_bucket");
        texture(assets, "entity/fish/cod", "entity/fish/salmon", "entity/fish/pufferfish", "entity/fish/tropical_a", "entity/fish/tropical_b");
        for (int i = 1; i <= 6; i++) texture(assets, "entity/fish/tropical_a_pattern_" + i, "entity/fish/tropical_b_pattern_" + i);
        texture(assets, "entity/turtle/big_sea_turtle", "entity/dolphin", "entity/zombie/drowned", "entity/zombie/drowned_outer_layer",
                "entity/phantom", "entity/phantom_eyes");
    }

    private static void addModels(Map<String, String> assets) {
        model(assets, "block/coral_fan", "block/coral_wall_fan", "block/template_seagrass", "block/template_wall_side_tall",
                "block/template_turtle_egg", "block/template_two_turtle_eggs", "block/template_three_turtle_eggs", "block/template_four_turtle_eggs",
                "block/sea_pickle", "block/two_sea_pickles", "block/three_sea_pickles", "block/four_sea_pickles",
                "block/dead_sea_pickle", "block/two_dead_sea_pickles", "block/three_dead_sea_pickles", "block/four_dead_sea_pickles");
    }

    private static void addSounds(Map<String, String> assets) {
        sounds(assets, "ambient/underwater/enter", 3);
        sounds(assets, "ambient/underwater/exit", 3);
        sound(assets, "ambient/underwater/underwater_ambience");
        sounds(assets, "ambient/underwater/additions/bubbles", 6);
        sounds(assets, "ambient/underwater/additions/water", 2);
        sounds(assets, "ambient/underwater/additions/animal", 2);
        sounds(assets, "ambient/underwater/additions/bass_whale", 2);
        sounds(assets, "ambient/underwater/additions/crackles", 2);
        sounds(assets, "ambient/underwater/additions/driplets", 2);
        sound(assets, "ambient/underwater/additions/earth_crack");
        sounds(assets, "ambient/underwater/additions/dark", 4);

        sounds(assets, "block/bubble_column/bubble", 3);
        sounds(assets, "block/bubble_column/upwards_ambient", 5);
        sound(assets, "block/bubble_column/upwards_inside");
        sounds(assets, "block/bubble_column/whirlpool_ambient", 5);
        sound(assets, "block/bubble_column/whirlpool_inside");

        sound(assets, "block/conduit/activate");
        sound(assets, "block/conduit/ambient");
        sounds(assets, "block/conduit/short", 9);
        sounds(assets, "block/conduit/attack", 3);
        sound(assets, "block/conduit/deactivate");

        sounds(assets, "dig/coral", 4);
        sounds(assets, "step/coral", 6);
        sounds(assets, "dig/wet_grass", 4);
        sounds(assets, "step/wet_grass", 6);
        sounds(assets, "block/pumpkin/carve", 2);

        sounds(assets, "mob/turtle/egg/egg_break", 2);
        sounds(assets, "mob/turtle/egg/egg_crack", 5);
        sounds(assets, "mob/turtle/baby/egg_hatched", 3);
        sound(assets, "mob/turtle/armor");
        sounds(assets, "entity/squid/squirt", 3);
        sounds(assets, "mob/husk/convert", 2);
        sounds(assets, "item/axe/strip", 4);

        sounds(assets, "item/trident/throw", 2);
        sounds(assets, "item/trident/pierce", 3);
        sounds(assets, "item/trident/ground_impact", 4);
        sounds(assets, "item/trident/return", 3);
        sounds(assets, "item/trident/riptide", 3);
        sounds(assets, "item/trident/thunder", 2);

        sounds(assets, "entity/fish/flop", 4);
        sounds(assets, "entity/fish/hurt", 4);
        sounds(assets, "entity/fish/swim", 7);
        sounds(assets, "entity/pufferfish/blow_out", 2);
        sounds(assets, "entity/pufferfish/blow_up", 2);
        sounds(assets, "entity/pufferfish/death", 2);
        sounds(assets, "entity/pufferfish/flop", 4);
        sounds(assets, "entity/pufferfish/hurt", 2);
        sounds(assets, "entity/pufferfish/sting", 2);

        sounds(assets, "mob/dolphin/idle", 6);
        sounds(assets, "mob/dolphin/idle_water", 10);
        sounds(assets, "mob/dolphin/attack", 3);
        sounds(assets, "mob/dolphin/death", 2);
        sounds(assets, "mob/dolphin/eat", 3);
        sounds(assets, "mob/dolphin/hurt", 3);
        sounds(assets, "mob/dolphin/jump", 3);
        sounds(assets, "mob/dolphin/play", 2);
        sounds(assets, "mob/dolphin/splash", 3);
        sounds(assets, "mob/dolphin/swim", 4);

        sounds(assets, "mob/turtle/idle", 3);
        sounds(assets, "mob/turtle/death", 3);
        sounds(assets, "mob/turtle/hurt", 5);
        sounds(assets, "mob/turtle/walk", 5);
        sounds(assets, "mob/turtle/swim/swim", 5);
        sounds(assets, "mob/turtle/baby/death", 2);
        sounds(assets, "mob/turtle/baby/hurt", 2);
        sounds(assets, "mob/turtle/baby/shamble", 4);
        sounds(assets, "mob/turtle/egg/drop_egg", 2);
        sounds(assets, "mob/turtle/egg/jump_egg", 4);

        sounds(assets, "mob/drowned/idle", 5);
        sounds(assets, "mob/drowned/death", 2);
        sounds(assets, "mob/drowned/hurt", 3);
        sounds(assets, "mob/drowned/step", 5);
        sounds(assets, "mob/drowned/convert", 3);
        sounds(assets, "mob/drowned/water/idle", 4);
        sounds(assets, "mob/drowned/water/death", 2);
        sounds(assets, "mob/drowned/water/hurt", 3);

        sounds(assets, "mob/phantom/idle", 5);
        sounds(assets, "mob/phantom/bite", 2);
        sounds(assets, "mob/phantom/death", 3);
        sounds(assets, "mob/phantom/flap", 6);
        sounds(assets, "mob/phantom/hurt", 3);
        sounds(assets, "mob/phantom/swoop", 4);

        sounds(assets, "item/bucket/empty_fish", 3);
        sounds(assets, "item/bucket/fill_fish", 3);
    }

    // ---- Helpers ----

    private static void texture(Map<String, String> assets, String... paths) {
        for (String path : paths) {
            String target = path.replaceFirst("^block/", "blocks/").replaceFirst("^item/", "items/");
            assets.put("assets/minecraft/textures/" + path + ".png", "assets/" + Tags.MOD_ID + "/textures/" + target + ".png");
        }
    }

    private static void animatedTexture(Map<String, String> assets, String... paths) {
        for (String path : paths) {
            texture(assets, path);
            String target = path.replaceFirst("^block/", "blocks/");
            assets.put("assets/minecraft/textures/" + path + ".png.mcmeta", "assets/" + Tags.MOD_ID + "/textures/" + target + ".png.mcmeta");
        }
    }

    private static void model(Map<String, String> assets, String... paths) {
        for (String path : paths) {
            assets.put("assets/minecraft/models/" + path + ".json", "assets/" + Tags.MOD_ID + "/models/" + path + ".json");
        }
    }

    /** A vanilla model kept under another name in Portium's namespace, for one of Portium's models to build on. */
    private static void modelAs(Map<String, String> assets, String path, String target) {
        assets.put("assets/minecraft/models/" + path + ".json", "assets/" + Tags.MOD_ID + "/models/" + target + ".json");
    }

    private static void sound(Map<String, String> assets, String path) {
        assets.put("assets/minecraft/sounds/" + path + ".ogg", "assets/" + Tags.MOD_ID + "/sounds/" + path + ".ogg");
    }

    private static void sounds(Map<String, String> assets, String prefix, int count) {
        for (int i = 1; i <= count; i++) sound(assets, prefix + i);
    }

    private PortiumAssets() {}
}
