package com.michaelsebero.portium.mods116;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 1.12's biomes as Forge 1.16's biome loading describes them, for the 1.16 glue (which can't name 1.12's classes). */
public final class ModernBiomes116 {

    /**
     * What 1.16's biome loading tells mods of each of 1.12's biomes: its name, 1.16's category (by name), its
     * precipitation (1.16's rain type, by name), temperature, downfall, depth and scale.
     */
    public static List<Object[]> biomes() {
        List<Object[]> biomes = new ArrayList<>();
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (biome.getRegistryName() == null) continue;
            String precipitation = biome.isSnowyBiome() ? "SNOW" : biome.canRain() ? "RAIN" : "NONE";
            biomes.add(new Object[] {biome.getRegistryName().toString(), category(biome), precipitation, biome.getDefaultTemperature(), biome.getRainfall(),
                    biome.getBaseHeight(), biome.getHeightVariation()});
        }
        return biomes;
    }

    /** 1.16's category of a 1.12 biome, from Forge's biome dictionary. */
    static String category(Biome biome) {
        Set<BiomeDictionary.Type> types = BiomeDictionary.hasAnyType(biome) ? BiomeDictionary.getTypes(biome) : java.util.Collections.emptySet();
        if (types.contains(BiomeDictionary.Type.NETHER)) return "NETHER";
        if (types.contains(BiomeDictionary.Type.END)) return "THEEND";
        if (types.contains(BiomeDictionary.Type.VOID)) return "NONE";
        if (types.contains(BiomeDictionary.Type.MUSHROOM)) return "MUSHROOM";
        if (types.contains(BiomeDictionary.Type.OCEAN)) return "OCEAN";
        if (types.contains(BiomeDictionary.Type.RIVER)) return "RIVER";
        if (types.contains(BiomeDictionary.Type.BEACH)) return "BEACH";
        if (types.contains(BiomeDictionary.Type.SWAMP)) return "SWAMP";
        if (types.contains(BiomeDictionary.Type.JUNGLE)) return "JUNGLE";
        if (types.contains(BiomeDictionary.Type.MESA)) return "MESA";
        if (types.contains(BiomeDictionary.Type.SAVANNA)) return "SAVANNA";
        if (types.contains(BiomeDictionary.Type.SANDY) && types.contains(BiomeDictionary.Type.HOT)) return "DESERT";
        if (types.contains(BiomeDictionary.Type.CONIFEROUS)) return "TAIGA";
        if (types.contains(BiomeDictionary.Type.SNOWY)) return "ICY";
        if (types.contains(BiomeDictionary.Type.MOUNTAIN)) return "EXTREME_HILLS";
        if (types.contains(BiomeDictionary.Type.FOREST)) return "FOREST";
        if (types.contains(BiomeDictionary.Type.PLAINS)) return "PLAINS";
        return "NONE";
    }

    private ModernBiomes116() {}
}
