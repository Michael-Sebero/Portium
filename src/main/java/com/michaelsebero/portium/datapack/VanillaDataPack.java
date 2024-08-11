package com.michaelsebero.portium.datapack;

import com.cleanroommc.assetmover.AssetMoverAPI;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The built-in {@code vanilla} pack: Portium's bundled files, over 1.13.2's own tags, advancements and loot tables,
 * which AssetMover extracts from the official jar into {@code assetmover/} (Portium ships no Mojang data). Being a
 * data pack, it replaces 1.12's advancements and loot tables with 1.13's.
 */
public class VanillaDataPack extends DataPack {

    private static final String DATA_VERSION = "1.13.2";
    private static final File EXTRACTED = new File("assetmover/assets/" + Tags.MOD_ID + "/vanilla_data");

    private static final String[] BLOCK_TAGS = {"acacia_logs", "anvil", "banners", "birch_logs", "buttons", "carpets", "coral_blocks", "coral_plants", "corals",
            "dark_oak_logs", "doors", "enderman_holdable", "flower_pots", "ice", "impermeable", "jungle_logs", "leaves", "logs", "oak_logs", "planks", "rails",
            "sand", "saplings", "slabs", "spruce_logs", "stairs", "stone_bricks", "trapdoors", "underwater_bonemeals", "valid_spawn", "wall_corals",
            "wooden_buttons", "wooden_doors", "wooden_pressure_plates", "wooden_slabs", "wooden_stairs", "wooden_trapdoors", "wool"};
    private static final String[] ITEM_TAGS = {"acacia_logs", "anvil", "banners", "birch_logs", "boats", "buttons", "carpets", "dark_oak_logs", "doors", "fishes",
            "jungle_logs", "leaves", "logs", "oak_logs", "planks", "rails", "sand", "saplings", "slabs", "spruce_logs", "stairs", "stone_bricks", "trapdoors",
            "wooden_buttons", "wooden_doors", "wooden_pressure_plates", "wooden_slabs", "wooden_stairs", "wooden_trapdoors", "wool"};
    private static final String[] FLUID_TAGS = {"lava", "water"};

    /** 1.13.2's advancement tabs. Its recipe-unlocking advancements name 1.13 recipes, so 1.12's own are kept. */
    private static final String[] ADVANCEMENTS = {
            "story/root", "story/mine_stone", "story/upgrade_tools", "story/smelt_iron", "story/obtain_armor", "story/lava_bucket", "story/iron_tools",
            "story/deflect_arrow", "story/form_obsidian", "story/mine_diamond", "story/enter_the_nether", "story/shiny_gear", "story/enchant_item",
            "story/cure_zombie_villager", "story/follow_ender_eye", "story/enter_the_end",
            "nether/root", "nether/return_to_sender", "nether/find_fortress", "nether/fast_travel", "nether/uneasy_alliance", "nether/get_wither_skull",
            "nether/summon_wither", "nether/brew_potion", "nether/create_beacon", "nether/all_potions", "nether/create_full_beacon", "nether/all_effects",
            "end/root", "end/kill_dragon", "end/dragon_egg", "end/enter_end_gateway", "end/respawn_dragon", "end/dragon_breath", "end/find_end_city",
            "end/elytra", "end/levitate",
            "adventure/root", "adventure/kill_a_mob", "adventure/trade", "adventure/sleep_in_bed", "adventure/shoot_arrow", "adventure/kill_all_mobs",
            "adventure/totem_of_undying", "adventure/summon_iron_golem", "adventure/adventuring_time", "adventure/very_very_frightening",
            "adventure/sniper_duel", "adventure/throw_trident",
            "husbandry/root", "husbandry/plant_seed", "husbandry/breed_an_animal", "husbandry/tame_an_animal", "husbandry/fishy_business",
            "husbandry/bred_all_animals", "husbandry/balanced_diet", "husbandry/break_diamond_hoe", "husbandry/tactical_fishing"};

    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
            "blue", "brown", "green", "red", "black"};

    /** 1.13.2's loot tables. Blocks only got loot tables in 1.14. */
    private static final String[] LOOT_TABLES = {
            "entities/bat", "entities/blaze", "entities/cave_spider", "entities/chicken", "entities/cod", "entities/cow", "entities/creeper",
            "entities/dolphin", "entities/donkey", "entities/drowned", "entities/elder_guardian", "entities/ender_dragon", "entities/enderman",
            "entities/endermite", "entities/evoker", "entities/ghast", "entities/giant", "entities/guardian", "entities/horse", "entities/husk",
            "entities/iron_golem", "entities/llama", "entities/magma_cube", "entities/mushroom_cow", "entities/mule",
            "entities/ocelot", "entities/parrot", "entities/phantom", "entities/pig", "entities/polar_bear", "entities/pufferfish", "entities/rabbit",
            "entities/salmon", "entities/sheep", "entities/shulker", "entities/silverfish", "entities/skeleton", "entities/skeleton_horse",
            "entities/slime", "entities/snow_golem", "entities/spider", "entities/squid", "entities/stray", "entities/tropical_fish", "entities/turtle",
            "entities/vex", "entities/villager", "entities/vindicator", "entities/witch", "entities/wither_skeleton", "entities/wolf", "entities/zombie",
            "entities/zombie_horse", "entities/zombie_pigman", "entities/zombie_villager",
            "chests/abandoned_mineshaft", "chests/buried_treasure", "chests/desert_pyramid", "chests/end_city_treasure", "chests/igloo_chest",
            "chests/jungle_temple", "chests/jungle_temple_dispenser", "chests/nether_bridge", "chests/shipwreck_map", "chests/shipwreck_supply",
            "chests/shipwreck_treasure", "chests/simple_dungeon", "chests/spawn_bonus_chest", "chests/stronghold_corridor", "chests/stronghold_crossing",
            "chests/stronghold_library", "chests/underwater_ruin_big", "chests/underwater_ruin_small", "chests/village_blacksmith",
            "chests/woodland_mansion",
            "gameplay/fishing", "gameplay/fishing/fish", "gameplay/fishing/junk", "gameplay/fishing/treasure"};

    private final DataPack bundled;
    private final FolderDataPack extracted = new FolderDataPack(DataPackRepository.VANILLA, EXTRACTED);

    public VanillaDataPack(DataPack bundled) {
        super(DataPackRepository.VANILLA);
        this.bundled = bundled;
    }

    /** Asks AssetMover for 1.13.2's vanilla data. Must run during mod construction. */
    public static void fetch() {
        if (!Loader.isModLoaded("assetmover")) {
            Portium.LOGGER.warn("AssetMover is not installed, so data packs won't have 1.13's vanilla tags.");
            return;
        }
        Map<String, String> files = new LinkedHashMap<>();
        tags(files, "blocks", BLOCK_TAGS);
        tags(files, "items", ITEM_TAGS);
        tags(files, "fluids", FLUID_TAGS);
        data(files, "advancements", ".json", ADVANCEMENTS);
        data(files, "loot_tables", ".json", LOOT_TABLES);
        for (String color : COLORS) data(files, "loot_tables", ".json", "entities/sheep/" + color);
        AssetMoverAPI.fromMinecraft(DATA_VERSION, files);
    }

    private static void tags(Map<String, String> files, String type, String[] names) {
        data(files, "tags/" + type, ".json", names);
    }

    private static void data(Map<String, String> files, String folder, String extension, String... names) {
        for (String name : names) {
            String path = "data/minecraft/" + folder + "/" + name + extension;
            files.put(path, "assets/" + Tags.MOD_ID + "/vanilla_data/" + path);
        }
    }

    @Nullable
    @Override
    public InputStream open(String path) throws IOException {
        InputStream stream = this.bundled.open(path);
        return stream != null ? stream : this.extracted.open(path);
    }

    @Override
    public Collection<ResourceLocation> list(String folder, String extension) {
        Set<ResourceLocation> ids = new LinkedHashSet<>(this.bundled.list(folder, extension));
        ids.addAll(this.extracted.list(folder, extension));
        return ids;
    }

    @Override
    public void close() throws IOException {
        this.bundled.close();
    }
}
