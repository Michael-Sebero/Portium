package com.michaelsebero.portium.client;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.13 water and underwater-fog colours per biome. 1.13 tints a grayscale water texture with these, where 1.12
 * multiplied an already blue texture. Biomes without an entry get 1.13's default, or an approximation of their
 * 1.12 tint if a mod gave them one.
 */
public final class WaterColors {

    public static final int DEFAULT_WATER = 0x3F76E4;
    public static final int DEFAULT_FOG = 0x050533;
    private static final int LEGACY_DEFAULT_MULTIPLIER = 0xFFFFFF;
    /** The average colour of 1.12's water texture, used to translate modded 1.12 tints. */
    private static final int LEGACY_TEXTURE_COLOR = 0x2B3BF4;

    private static final Map<ResourceLocation, int[]> COLORS = new HashMap<>();

    static {
        put("minecraft:swampland", 0x617B64, 0x232317);
        put("minecraft:mutated_swampland", 0x617B64, 0x232317);
        put("minecraft:frozen_river", 0x3938C9, DEFAULT_FOG);
        put("minecraft:frozen_ocean", 0x3938C9, DEFAULT_FOG);
        put("minecraft:cold_beach", 0x3D57D6, DEFAULT_FOG);
        put("minecraft:taiga_cold", 0x3D57D6, DEFAULT_FOG);
        put("minecraft:taiga_cold_hills", 0x3D57D6, DEFAULT_FOG);
        put("minecraft:mutated_taiga_cold", 0x3D57D6, DEFAULT_FOG);
        put("portium:warm_ocean", 0x43D5EE, 0x041F33);
        put("portium:deep_warm_ocean", 0x43D5EE, 0x041F33);
        put("portium:lukewarm_ocean", 0x45ADF2, 0x041633);
        put("portium:deep_lukewarm_ocean", 0x45ADF2, 0x041633);
        put("portium:cold_ocean", 0x3D57D6, DEFAULT_FOG);
        put("portium:deep_cold_ocean", 0x3D57D6, DEFAULT_FOG);
        put("portium:deep_frozen_ocean", 0x3938C9, DEFAULT_FOG);

        // Popular mods' swampy and magical biomes, matched to their later-version colours.
        put("biomesoplenty:bayou", 0x62AF84, 0x0C211C);
        put("biomesoplenty:dead_swamp", 0x354762, 0x040511);
        put("biomesoplenty:mangrove", 0x448FBD, 0x061326);
        put("biomesoplenty:mystic_grove", 0x9C3FE4, 0x2E0533);
        put("biomesoplenty:ominous_woods", 0x312346, 0x0A030C);
        put("biomesoplenty:tropical_rainforest", 0x1FA14A, 0x02271A);
        put("biomesoplenty:quagmire", 0x433721, 0x0C0C03);
        put("biomesoplenty:wetland", 0x272179, 0x0C031B);
        put("biomesoplenty:bog", DEFAULT_WATER, DEFAULT_FOG);
        put("biomesoplenty:moor", DEFAULT_WATER, DEFAULT_FOG);
        put("thebetweenlands:swamplands", 0x184220, 0x184220);
        put("thebetweenlands:swamplands_clearing", 0x184220, 0x184220);
        put("thebetweenlands:coarse_islands", 0x1B3944, 0x1B3944);
        put("thebetweenlands:deep_waters", 0x1B3944, 0x1B3944);
        put("thebetweenlands:marsh_0", 0x485E18, 0x485E18);
        put("thebetweenlands:marsh_1", 0x485E18, 0x485E18);
        put("thebetweenlands:patchy_islands", 0x184220, 0x184220);
        put("thebetweenlands:raised_isles", 0x1B3944, 0x1B3944);
        put("thebetweenlands:sludge_plains", 0x3A2F0B, 0x3A2F0B);
        put("thebetweenlands:sludge_plains_clearing", 0x3A2F0B, 0x3A2F0B);
        put("traverse:mini_jungle", 0x003320, 0x052721);
        put("traverse:green_swamp", 0x617B64, 0x232317);
        put("thaumcraft:magical_forest", 0x2E535F, DEFAULT_FOG);
        put("thaumcraft:eerie", 0x2E535F, DEFAULT_FOG);
        put("integrateddynamics:biome_meneglin", DEFAULT_WATER, 0x55A8DD);
    }

    private static void put(String biome, int water, int fog) {
        COLORS.put(new ResourceLocation(biome), new int[] {water, fog});
    }

    /** The 1.13 water colour for a biome, given the 1.12 multiplier the biome (or another mod) reports. */
    public static int waterColor(Biome biome, int legacyMultiplier) {
        int[] colors = COLORS.get(biome.getRegistryName());
        if (colors != null) return colors[0];
        if ((legacyMultiplier & 0xFFFFFF) == LEGACY_DEFAULT_MULTIPLIER) return DEFAULT_WATER;
        return multiply(legacyMultiplier, LEGACY_TEXTURE_COLOR);
    }

    public static int fogColor(Biome biome) {
        int[] colors = COLORS.get(biome.getRegistryName());
        return colors != null ? colors[1] : DEFAULT_FOG;
    }

    private static int multiply(int a, int b) {
        int r = ((a >> 16) & 255) * ((b >> 16) & 255) / 255;
        int g = ((a >> 8) & 255) * ((b >> 8) & 255) / 255;
        int bl = (a & 255) * (b & 255) / 255;
        return (r << 16) | (g << 8) | bl;
    }

    private WaterColors() {}
}
