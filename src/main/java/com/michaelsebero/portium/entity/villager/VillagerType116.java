package com.michaelsebero.portium.entity.villager;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.Locale;
import java.util.Set;

/** 1.14's villager types, which dress villagers (and zombie villagers) by the biome they were born in. */
public enum VillagerType116 {
    DESERT, JUNGLE, PLAINS, SAVANNA, SNOW, SWAMP, TAIGA;

    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static VillagerType116 byName(String name) {
        String bare = name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
        for (VillagerType116 type : values()) {
            if (type.getName().equals(bare)) return type;
        }
        return PLAINS;
    }

    /**
     * The type of a biome, as 1.14 maps them: badlands and deserts dress as desert, jungles (and bamboo jungles) as
     * jungle, savannas as savanna, snowy biomes as snow, swamps as swamp, taigas as taiga; anything else, plains.
     * Read from Forge's biome dictionary so modded biomes dress sensibly too.
     */
    public static VillagerType116 byBiome(Biome biome) {
        Set<BiomeDictionary.Type> types = BiomeDictionary.getTypes(biome);
        if (types.contains(BiomeDictionary.Type.SNOWY)) return SNOW;
        if (types.contains(BiomeDictionary.Type.MESA) || types.contains(BiomeDictionary.Type.SANDY) && types.contains(BiomeDictionary.Type.HOT)) {
            return DESERT;
        }
        if (types.contains(BiomeDictionary.Type.JUNGLE)) return JUNGLE;
        if (types.contains(BiomeDictionary.Type.SAVANNA)) return SAVANNA;
        if (types.contains(BiomeDictionary.Type.SWAMP)) return SWAMP;
        if (types.contains(BiomeDictionary.Type.CONIFEROUS) && !types.contains(BiomeDictionary.Type.MOUNTAIN)) return TAIGA;
        return PLAINS;
    }
}
