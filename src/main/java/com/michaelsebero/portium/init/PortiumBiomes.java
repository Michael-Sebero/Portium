package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.world.biome.BiomeFrozenOcean;
import com.michaelsebero.portium.world.biome.BiomeNether116;
import com.michaelsebero.portium.world.biome.BiomePortiumOcean;
import com.michaelsebero.portium.world.biome.OceanTemperature;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * The 1.13 ocean biomes. {@code minecraft:frozen_ocean} is replaced (keeping its ID) because 1.12 never generated it
 * and it needs the 1.13 surface. Placement happens in {@link com.michaelsebero.portium.world.gen.layer.GenLayerMixOceans}.
 * <p>
 * Also 1.16's Nether biomes, each registered only when the blocks it's made of are; the 1.16 Nether generator
 * places them.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumBiomes {

    public static Biome WARM_OCEAN;
    public static Biome LUKEWARM_OCEAN;
    public static Biome COLD_OCEAN;
    public static Biome FROZEN_OCEAN;
    public static Biome DEEP_WARM_OCEAN;
    public static Biome DEEP_LUKEWARM_OCEAN;
    public static Biome DEEP_COLD_OCEAN;
    public static Biome DEEP_FROZEN_OCEAN;
    public static Biome SOUL_SAND_VALLEY;
    public static Biome CRIMSON_FOREST;
    public static Biome WARPED_FOREST;
    public static Biome BASALT_DELTAS;

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        IForgeRegistry<Biome> registry = event.getRegistry();
        WARM_OCEAN = register(registry, new ResourceLocation(Tags.MOD_ID, "warm_ocean"),
                new BiomePortiumOcean(ocean("Warm Ocean", false, 0.5F), OceanTemperature.WARM, false), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.HOT, BiomeDictionary.Type.WET);
        LUKEWARM_OCEAN = register(registry, new ResourceLocation(Tags.MOD_ID, "lukewarm_ocean"),
                new BiomePortiumOcean(ocean("Lukewarm Ocean", false, 0.5F), OceanTemperature.LUKEWARM, false), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.WET);
        COLD_OCEAN = register(registry, new ResourceLocation(Tags.MOD_ID, "cold_ocean"),
                new BiomePortiumOcean(ocean("Cold Ocean", false, 0.5F), OceanTemperature.COLD, false), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.COLD, BiomeDictionary.Type.WET);
        FROZEN_OCEAN = register(registry, new ResourceLocation("minecraft", "frozen_ocean"),
                new BiomeFrozenOcean(ocean("Frozen Ocean", false, 0.0F).setSnowEnabled(), false), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.COLD, BiomeDictionary.Type.SNOWY);
        DEEP_WARM_OCEAN = register(registry, new ResourceLocation(Tags.MOD_ID, "deep_warm_ocean"),
                new BiomePortiumOcean(ocean("Deep Warm Ocean", true, 0.5F), OceanTemperature.WARM, true), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.HOT, BiomeDictionary.Type.WET);
        DEEP_LUKEWARM_OCEAN = register(registry, new ResourceLocation(Tags.MOD_ID, "deep_lukewarm_ocean"),
                new BiomePortiumOcean(ocean("Deep Lukewarm Ocean", true, 0.5F), OceanTemperature.LUKEWARM, true), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.WET);
        DEEP_COLD_OCEAN = register(registry, new ResourceLocation(Tags.MOD_ID, "deep_cold_ocean"),
                new BiomePortiumOcean(ocean("Deep Cold Ocean", true, 0.5F), OceanTemperature.COLD, true), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.COLD, BiomeDictionary.Type.WET);
        DEEP_FROZEN_OCEAN = register(registry, new ResourceLocation(Tags.MOD_ID, "deep_frozen_ocean"),
                new BiomeFrozenOcean(ocean("Deep Frozen Ocean", true, 0.5F), true), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.COLD, BiomeDictionary.Type.SNOWY);
        registerNether(registry);
    }

    private static void registerNether(IForgeRegistry<Biome> registry) {
        if (!PortiumConfig.worldGen.netherUpdate) return;
        PortiumConfig.Blocks blocks = PortiumConfig.blocks;
        if (blocks.soulSoil) {
            SOUL_SAND_VALLEY = register(registry, new ResourceLocation(Tags.MOD_ID, "soul_sand_valley"),
                    new BiomeNether116("Soul Sand Valley", BiomeNether116.Kind.SOUL_SAND_VALLEY), BiomeDictionary.Type.NETHER, BiomeDictionary.Type.HOT,
                    BiomeDictionary.Type.DRY, BiomeDictionary.Type.SPOOKY);
        }
        if (blocks.netherForests) {
            CRIMSON_FOREST = register(registry, new ResourceLocation(Tags.MOD_ID, "crimson_forest"),
                    new BiomeNether116("Crimson Forest", BiomeNether116.Kind.CRIMSON_FOREST), BiomeDictionary.Type.NETHER, BiomeDictionary.Type.HOT,
                    BiomeDictionary.Type.DRY, BiomeDictionary.Type.FOREST);
            WARPED_FOREST = register(registry, new ResourceLocation(Tags.MOD_ID, "warped_forest"),
                    new BiomeNether116("Warped Forest", BiomeNether116.Kind.WARPED_FOREST), BiomeDictionary.Type.NETHER, BiomeDictionary.Type.HOT,
                    BiomeDictionary.Type.DRY, BiomeDictionary.Type.FOREST);
        }
        if (blocks.basalt && blocks.blackstone) {
            BASALT_DELTAS = register(registry, new ResourceLocation(Tags.MOD_ID, "basalt_deltas"),
                    new BiomeNether116("Basalt Deltas", BiomeNether116.Kind.BASALT_DELTAS), BiomeDictionary.Type.NETHER, BiomeDictionary.Type.HOT,
                    BiomeDictionary.Type.DRY);
        }
    }

    private static Biome.BiomeProperties ocean(String name, boolean deep, float temperature) {
        return new Biome.BiomeProperties(name).setBaseHeight(deep ? -1.8F : -1.0F).setHeightVariation(0.1F).setTemperature(temperature).setRainfall(0.5F);
    }

    private static Biome register(IForgeRegistry<Biome> registry, ResourceLocation name, Biome biome, BiomeDictionary.Type... types) {
        biome.setRegistryName(name);
        registry.register(biome);
        BiomeDictionary.addTypes(biome, types);
        return biome;
    }

    /**
     * The biome ID for an ocean of the given temperature, as 1.13 places them: warm water stays warm over deep
     * ocean (1.13 never generates deep warm oceans), the other temperatures get their deep variant.
     */
    public static int oceanId(OceanTemperature temperature, boolean deep) {
        Biome biome;
        switch (temperature) {
            case WARM: biome = WARM_OCEAN; break;
            case LUKEWARM: biome = deep ? DEEP_LUKEWARM_OCEAN : LUKEWARM_OCEAN; break;
            case COLD: biome = deep ? DEEP_COLD_OCEAN : COLD_OCEAN; break;
            case FROZEN: biome = deep ? DEEP_FROZEN_OCEAN : FROZEN_OCEAN; break;
            default: biome = deep ? Biomes.DEEP_OCEAN : Biomes.OCEAN; break;
        }
        return Biome.getIdForBiome(biome);
    }

    private PortiumBiomes() {}
}
