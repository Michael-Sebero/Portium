package com.michaelsebero.portium.compat;

import com.michaelsebero.portium.Tags;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/** 1.13 renamed most biomes; its new ocean biomes are Portium's. */
public final class LegacyBiomes {

    private static final Map<String, String> RENAMED = new HashMap<>();
    private static final Map<String, String> MODERN = new HashMap<>();

    static {
        String[] pairs = {
                "mountains", "extreme_hills", "swamp", "swampland", "nether", "hell", "the_end", "sky", "snowy_tundra", "ice_flats",
                "snowy_mountains", "ice_mountains", "mushroom_fields", "mushroom_island", "mushroom_field_shore", "mushroom_island_shore",
                "beach", "beaches", "wooded_hills", "forest_hills", "mountain_edge", "smaller_extreme_hills", "stone_shore", "stone_beach",
                "snowy_beach", "cold_beach", "dark_forest", "roofed_forest", "snowy_taiga", "taiga_cold", "snowy_taiga_hills", "taiga_cold_hills",
                "giant_tree_taiga", "redwood_taiga", "giant_tree_taiga_hills", "redwood_taiga_hills", "wooded_mountains", "extreme_hills_with_trees",
                "savanna_plateau", "savanna_rock", "badlands", "mesa", "wooded_badlands_plateau", "mesa_rock", "badlands_plateau", "mesa_clear_rock",
                "the_void", "void", "sunflower_plains", "mutated_plains", "desert_lakes", "mutated_desert", "gravelly_mountains", "mutated_extreme_hills",
                "flower_forest", "mutated_forest", "taiga_mountains", "mutated_taiga", "swamp_hills", "mutated_swampland", "ice_spikes", "mutated_ice_flats",
                "modified_jungle", "mutated_jungle", "modified_jungle_edge", "mutated_jungle_edge", "tall_birch_forest", "mutated_birch_forest",
                "tall_birch_hills", "mutated_birch_forest_hills", "dark_forest_hills", "mutated_roofed_forest", "snowy_taiga_mountains", "mutated_taiga_cold",
                "giant_spruce_taiga", "mutated_redwood_taiga", "giant_spruce_taiga_hills", "mutated_redwood_taiga_hills",
                "modified_gravelly_mountains", "mutated_extreme_hills_with_trees", "shattered_savanna", "mutated_savanna",
                "shattered_savanna_plateau", "mutated_savanna_rock", "eroded_badlands", "mutated_mesa", "modified_wooded_badlands_plateau", "mutated_mesa_rock",
                "modified_badlands_plateau", "mutated_mesa_clear_rock"};
        for (int i = 0; i < pairs.length; i += 2) {
            RENAMED.put(pairs[i], pairs[i + 1]);
            MODERN.put(pairs[i + 1], pairs[i]);
        }
    }

    /** The 1.12 registry id for a 1.13 biome id; ids it doesn't know pass through. */
    public static String toLegacy(String id) {
        ResourceLocation modern = new ResourceLocation(id);
        if (!"minecraft".equals(modern.getNamespace())) return id;
        String renamed = RENAMED.get(modern.getPath());
        if (renamed != null) return "minecraft:" + renamed;
        ResourceLocation portium = new ResourceLocation(Tags.MOD_ID, modern.getPath());
        return ForgeRegistries.BIOMES.containsKey(portium) ? portium.toString() : modern.toString();
    }

    /** 1.13's numbers for the biomes it added (40 to 50); every other vanilla biome kept its 1.12 number. */
    private static final String[] ADDED = {"small_end_islands", "end_midlands", "end_highlands", "end_barrens", "warm_ocean", "lukewarm_ocean",
            "cold_ocean", "deep_warm_ocean", "deep_lukewarm_ocean", "deep_cold_ocean", "deep_frozen_ocean"};

    /** Numbers 1.14 to 1.16 gave their new biomes (168 on), and the 1.12 biome to fall back on for each. */
    private static final String[] ADDED_LATER = {"bamboo_jungle", "bamboo_jungle_hills", "soul_sand_valley", "crimson_forest", "warped_forest",
            "basalt_deltas"};
    private static final int FIRST_ADDED_LATER = 168;

    /** The 1.12 biome for a biome number in a 1.13 to 1.16 chunk; the End's newer biomes are 1.12's single End biome. */
    public static Biome fromModernId(int id) {
        if (id >= FIRST_ADDED_LATER && id < FIRST_ADDED_LATER + ADDED_LATER.length) {
            Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(toLegacy("minecraft:" + ADDED_LATER[id - FIRST_ADDED_LATER])));
            if (biome != null) return biome;
            return id < FIRST_ADDED_LATER + 2 ? (id == FIRST_ADDED_LATER ? Biomes.JUNGLE : Biomes.JUNGLE_HILLS) : Biomes.HELL;
        }
        if (id >= 40 && id < 40 + ADDED.length) {
            Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(toLegacy("minecraft:" + ADDED[id - 40])));
            return biome != null ? biome : id < 44 ? Biomes.SKY : Biomes.OCEAN;
        }
        Biome biome = Biome.getBiome(id);
        return biome != null ? biome : Biomes.PLAINS;
    }

    /** The 1.13 id for a 1.12 or Portium biome; other mods' biomes keep theirs. */
    public static String toModern(ResourceLocation id) {
        if (Tags.MOD_ID.equals(id.getNamespace())) return "minecraft:" + id.getPath();
        if (!"minecraft".equals(id.getNamespace())) return id.toString();
        return "minecraft:" + MODERN.getOrDefault(id.getPath(), id.getPath());
    }

    private LegacyBiomes() {}
}
