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
    /** 1.16.5's files, kept apart from 1.13.2's (AssetMover won't replace a file it already extracted) and read first. */
    private static final File EXTRACTED_116 = new File("assetmover/assets/" + Tags.MOD_ID + "/vanilla_data_116");

    private static final String[] BLOCK_TAGS = {
            "acacia_logs", "anvil", "bamboo_plantable_on", "banners", "base_stone_nether", "base_stone_overworld", "beacon_base_blocks", "beds",
            "bee_growables", "beehives", "birch_logs", "buttons", "campfires", "carpets", "climbable", "coral_blocks", "coral_plants", "corals",
            "crimson_stems", "crops", "dark_oak_logs", "doors", "dragon_immune", "enderman_holdable", "fence_gates", "fences", "fire", "flower_pots",
            "flowers", "gold_ores", "guarded_by_piglins", "hoglin_repellents", "ice", "impermeable", "infiniburn_end", "infiniburn_nether",
            "infiniburn_overworld", "jungle_logs", "leaves", "logs", "logs_that_burn", "mushroom_grow_block", "non_flammable_wood", "nylium",
            "oak_logs", "piglin_repellents", "planks", "portals", "pressure_plates", "prevent_mob_spawning_inside", "rails", "sand", "saplings",
            "shulker_boxes", "signs", "slabs", "small_flowers", "soul_fire_base_blocks", "soul_speed_blocks", "spruce_logs", "stairs",
            "standing_signs", "stone_bricks", "stone_pressure_plates", "strider_warm_blocks", "tall_flowers", "trapdoors", "underwater_bonemeals",
            "unstable_bottom_center", "valid_spawn", "wall_corals", "wall_post_override", "wall_signs", "walls", "warped_stems", "wart_blocks",
            "wither_immune", "wither_summon_base_blocks", "wooden_buttons", "wooden_doors", "wooden_fences", "wooden_pressure_plates", "wooden_slabs",
            "wooden_stairs", "wooden_trapdoors", "wool"};
    private static final String[] ITEM_TAGS = {
            "acacia_logs", "anvil", "arrows", "banners", "beacon_payment_items", "beds", "birch_logs", "boats", "buttons", "carpets", "coals",
            "creeper_drop_music_discs", "crimson_stems", "dark_oak_logs", "doors", "fences", "fishes", "flowers", "gold_ores", "jungle_logs",
            "leaves", "lectern_books", "logs", "logs_that_burn", "music_discs", "non_flammable_wood", "oak_logs", "piglin_loved", "piglin_repellents",
            "planks", "rails", "sand", "saplings", "signs", "slabs", "small_flowers", "soul_fire_base_blocks", "spruce_logs", "stairs",
            "stone_bricks", "stone_crafting_materials", "stone_tool_materials", "tall_flowers", "trapdoors", "walls", "warped_stems",
            "wooden_buttons", "wooden_doors", "wooden_fences", "wooden_pressure_plates", "wooden_slabs", "wooden_stairs", "wooden_trapdoors", "wool"};
    private static final String[] FLUID_TAGS = {
            "lava", "water"};
    private static final String[] LOOT_TABLES_116 = {"chests/village/village_armorer", "chests/village/village_butcher",
            "chests/village/village_cartographer", "chests/village/village_mason", "chests/village/village_shepherd", "chests/village/village_tannery",
            "chests/village/village_weaponsmith", "chests/village/village_toolsmith", "chests/village/village_temple", "chests/village/village_fisher",
            "chests/village/village_fletcher", "chests/village/village_desert_house", "chests/village/village_plains_house",
            "chests/village/village_savanna_house", "chests/village/village_snowy_house", "chests/village/village_taiga_house", "chests/pillager_outpost",
            "chests/bastion_bridge", "chests/bastion_hoglin_stable", "chests/bastion_other", "chests/bastion_treasure", "chests/ruined_portal",
            // Changed since 1.13.2: bamboo and suspicious stew in chests, the turtle's bowl, fishing's jungle bamboo and lily pads.
            "chests/shipwreck_supply", "chests/jungle_temple", "entities/turtle", "gameplay/fishing/junk", "gameplay/fishing/treasure",
            // 1.14's gifts to the Hero of the Village, by profession.
            "gameplay/hero_of_the_village/armorer_gift", "gameplay/hero_of_the_village/butcher_gift", "gameplay/hero_of_the_village/cartographer_gift", "gameplay/hero_of_the_village/cleric_gift", "gameplay/hero_of_the_village/farmer_gift", "gameplay/hero_of_the_village/fisherman_gift", "gameplay/hero_of_the_village/fletcher_gift", "gameplay/hero_of_the_village/leatherworker_gift", "gameplay/hero_of_the_village/librarian_gift", "gameplay/hero_of_the_village/mason_gift", "gameplay/hero_of_the_village/shepherd_gift", "gameplay/hero_of_the_village/toolsmith_gift", "gameplay/hero_of_the_village/weaponsmith_gift"};
    private static final String[] ENTITY_TYPE_TAGS_116 = {"arrows", "beehive_inhabitors", "impact_projectiles", "raiders", "skeletons"};

    /**
     * 1.16.5's advancement tabs (1.14's raids and crossbows, 1.15's bees, 1.16's Nether). Their recipe-unlocking
     * advancements name 1.16 recipes, so 1.12's own are kept.
     */
    private static final String[] ADVANCEMENTS = {
            "adventure/adventuring_time", "adventure/arbalistic", "adventure/bullseye", "adventure/hero_of_the_village",
            "adventure/honey_block_slide", "adventure/kill_all_mobs", "adventure/kill_a_mob", "adventure/ol_betsy", "adventure/root",
            "adventure/shoot_arrow", "adventure/sleep_in_bed", "adventure/sniper_duel", "adventure/summon_iron_golem", "adventure/throw_trident",
            "adventure/totem_of_undying", "adventure/trade", "adventure/two_birds_one_arrow", "adventure/very_very_frightening",
            "adventure/voluntary_exile", "adventure/whos_the_pillager_now", "end/dragon_breath", "end/dragon_egg", "end/elytra",
            "end/enter_end_gateway", "end/find_end_city", "end/kill_dragon", "end/levitate", "end/respawn_dragon", "end/root",
            "husbandry/balanced_diet", "husbandry/bred_all_animals", "husbandry/breed_an_animal", "husbandry/complete_catalogue",
            "husbandry/fishy_business", "husbandry/obtain_netherite_hoe", "husbandry/plant_seed", "husbandry/root", "husbandry/safely_harvest_honey",
            "husbandry/silk_touch_nest", "husbandry/tactical_fishing", "husbandry/tame_an_animal", "nether/all_effects", "nether/all_potions",
            "nether/brew_potion", "nether/charge_respawn_anchor", "nether/create_beacon", "nether/create_full_beacon", "nether/distract_piglin",
            "nether/explore_nether", "nether/fast_travel", "nether/find_bastion", "nether/find_fortress", "nether/get_wither_skull",
            "nether/loot_bastion", "nether/netherite_armor", "nether/obtain_ancient_debris", "nether/obtain_blaze_rod",
            "nether/obtain_crying_obsidian", "nether/return_to_sender", "nether/ride_strider", "nether/root", "nether/summon_wither",
            "nether/uneasy_alliance", "nether/use_lodestone", "story/cure_zombie_villager", "story/deflect_arrow", "story/enchant_item",
            "story/enter_the_end", "story/enter_the_nether", "story/follow_ender_eye", "story/form_obsidian", "story/iron_tools", "story/lava_bucket",
            "story/mine_diamond", "story/mine_stone", "story/obtain_armor", "story/root", "story/shiny_gear", "story/smelt_iron",
            "story/upgrade_tools"};

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
    private final FolderDataPack extracted116 = new FolderDataPack(DataPackRepository.VANILLA, EXTRACTED_116);

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
        data(files, "loot_tables", ".json", LOOT_TABLES);
        for (String color : COLORS) data(files, "loot_tables", ".json", "entities/sheep/" + color);
        AssetMoverAPI.fromMinecraft(DATA_VERSION, files);
        // 1.13.2 has no entity type tags; 1.16.5's (raiders, arrows...) are what Portium's 1.14+ mobs and blocks use, as
        // are its piglin tags.
        Map<String, String> entityTags = new LinkedHashMap<>();
        String into = "vanilla_data_116";
        tags(entityTags, into, "entity_types", ENTITY_TYPE_TAGS_116);
        // 1.16.5's block, item and fluid tags (more than 1.13.2's: logs with Nether stems, piglin and hoglin tags...).
        tags(entityTags, into, "blocks", BLOCK_TAGS);
        tags(entityTags, into, "items", ITEM_TAGS);
        tags(entityTags, into, "fluids", FLUID_TAGS);
        // The chests of 1.14+ villages, pillager outposts and bastions (the jigsaw structures) name 1.16.5's loot tables.
        dataInto(entityTags, into, "loot_tables", ".json", LOOT_TABLES_116);
        dataInto(entityTags, into, "advancements", ".json", ADVANCEMENTS);
        AssetMoverAPI.fromMinecraft("1.16.5", entityTags);
    }

    private static void tags(Map<String, String> files, String into, String type, String[] names) {
        dataInto(files, into, "tags/" + type, ".json", names);
    }

    private static void data(Map<String, String> files, String folder, String extension, String... names) {
        dataInto(files, "vanilla_data", folder, extension, names);
    }

    private static void dataInto(Map<String, String> files, String into, String folder, String extension, String... names) {
        for (String name : names) {
            String path = "data/minecraft/" + folder + "/" + name + extension;
            files.put(path, "assets/" + Tags.MOD_ID + "/" + into + "/" + path);
        }
    }

    @Nullable
    @Override
    public InputStream open(String path) throws IOException {
        InputStream stream = this.bundled.open(path);
        if (stream == null) stream = this.extracted116.open(path);
        return stream != null ? stream : this.extracted.open(path);
    }

    @Override
    public Collection<ResourceLocation> list(String folder, String extension) {
        Set<ResourceLocation> ids = new LinkedHashSet<>(this.bundled.list(folder, extension));
        ids.addAll(this.extracted116.list(folder, extension));
        ids.addAll(this.extracted.list(folder, extension));
        return ids;
    }

    @Override
    public void close() throws IOException {
        this.bundled.close();
    }
}
