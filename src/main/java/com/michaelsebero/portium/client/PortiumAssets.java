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
        AssetMoverAPI.fromMinecraft(VERSION_116, textures116);
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

    private static void sound(Map<String, String> assets, String path) {
        assets.put("assets/minecraft/sounds/" + path + ".ogg", "assets/" + Tags.MOD_ID + "/sounds/" + path + ".ogg");
    }

    private static void sounds(Map<String, String> assets, String prefix, int count) {
        for (int i = 1; i <= count; i++) sound(assets, prefix + i);
    }

    private PortiumAssets() {}
}
