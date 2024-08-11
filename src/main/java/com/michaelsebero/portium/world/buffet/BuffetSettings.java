package com.michaelsebero.portium.world.buffet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.compat.LegacyBiomes;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;

/**
 * A Buffet world's settings, kept in level.dat's {@code generatorOptions} as the JSON 1.13 keeps there:
 * <pre>{"biome_source":{"type":"minecraft:fixed","options":{"biomes":["minecraft:plains"]}},
 *  "chunk_generator":{"type":"minecraft:surface","options":{"default_block":"minecraft:stone","default_fluid":"minecraft:water"}}}</pre>
 * Reading follows 1.13: without a biome source with both a type and options the world is a single ocean, unknown
 * biomes are ocean, unknown blocks are air, and any generator but caves and floating islands is the surface one.
 * 1.13's screen only makes single-biome sources, but checkerboard sources (with a size) and the normal layered source
 * work too, as do other default blocks and fluids.
 */
public final class BuffetSettings {

    public enum Generator {
        SURFACE("minecraft:surface"), CAVES("minecraft:caves"), FLOATING_ISLANDS("minecraft:floating_islands");

        public final String id;

        Generator(String id) {
            this.id = id;
        }
    }

    private enum Source { FIXED, CHECKERBOARD, VANILLA_LAYERED, NONE }

    private final Source source;
    private final Biome[] biomes;
    private final int size;
    public final Generator generator;
    public final IBlockState defaultBlock;
    public final IBlockState defaultFluid;

    private BuffetSettings(Source source, Biome[] biomes, int size, Generator generator, IBlockState defaultBlock, IBlockState defaultFluid) {
        this.source = source;
        this.biomes = biomes;
        this.size = size;
        this.generator = generator;
        this.defaultBlock = defaultBlock;
        this.defaultFluid = defaultFluid;
    }

    public static BuffetSettings parse(String options) {
        JsonObject json = new JsonObject();
        if (options != null && !options.trim().isEmpty()) {
            try {
                JsonElement parsed = new JsonParser().parse(options);
                if (parsed.isJsonObject()) json = parsed.getAsJsonObject();
            } catch (RuntimeException e) {
                Portium.LOGGER.error("Couldn't read the Buffet world settings {}", options, e);
            }
        }
        Source source = Source.NONE;
        Biome[] biomes = {Biomes.OCEAN};
        int size = 2;
        JsonObject biomeSource = object(json, "biome_source");
        if (biomeSource != null && biomeSource.has("type") && object(biomeSource, "options") != null) {
            String type = biomeSource.get("type").getAsString();
            source = "minecraft:fixed".equals(type) ? Source.FIXED : "minecraft:checkerboard".equals(type) ? Source.CHECKERBOARD
                    : "minecraft:vanilla_layered".equals(type) ? Source.VANILLA_LAYERED : Source.NONE;
            JsonObject sourceOptions = object(biomeSource, "options");
            if (sourceOptions.has("biomes") && sourceOptions.get("biomes").isJsonArray()) {
                JsonArray list = sourceOptions.getAsJsonArray("biomes");
                if (list.size() > 0) {
                    biomes = new Biome[list.size()];
                    for (int i = 0; i < list.size(); i++) biomes[i] = biome(list.get(i).getAsString());
                }
            }
            if (sourceOptions.has("size")) size = sourceOptions.get("size").getAsInt();
        }
        IBlockState block = Blocks.STONE.getDefaultState();
        IBlockState fluid = Blocks.WATER.getDefaultState();
        Generator generator = Generator.SURFACE;
        JsonObject chunkGenerator = object(json, "chunk_generator");
        if (chunkGenerator != null) {
            JsonObject generatorOptions = object(chunkGenerator, "options");
            if (generatorOptions != null && generatorOptions.has("default_block")) block = block(generatorOptions.get("default_block").getAsString());
            if (generatorOptions != null && generatorOptions.has("default_fluid")) fluid = block(generatorOptions.get("default_fluid").getAsString());
            if (chunkGenerator.has("type")) {
                String type = chunkGenerator.get("type").getAsString();
                if (Generator.CAVES.id.equals(type)) generator = Generator.CAVES;
                if (Generator.FLOATING_ISLANDS.id.equals(type)) generator = Generator.FLOATING_ISLANDS;
            }
        }
        return new BuffetSettings(source, biomes, size, generator, block, fluid);
    }

    /** What 1.13's Buffet screen writes: one biome, the chosen generator, stone and water. */
    public static String write(Generator generator, Biome biome) {
        JsonObject biomeOptions = new JsonObject();
        JsonArray biomes = new JsonArray();
        biomes.add(LegacyBiomes.toModern(biome.getRegistryName()));
        biomeOptions.add("biomes", biomes);
        JsonObject biomeSource = new JsonObject();
        biomeSource.addProperty("type", "minecraft:fixed");
        biomeSource.add("options", biomeOptions);
        JsonObject generatorOptions = new JsonObject();
        generatorOptions.addProperty("default_block", "minecraft:stone");
        generatorOptions.addProperty("default_fluid", "minecraft:water");
        JsonObject chunkGenerator = new JsonObject();
        chunkGenerator.addProperty("type", generator.id);
        chunkGenerator.add("options", generatorOptions);
        JsonObject json = new JsonObject();
        json.add("biome_source", biomeSource);
        json.add("chunk_generator", chunkGenerator);
        return json.toString();
    }

    public BiomeProvider createBiomeProvider(World world) {
        switch (this.source) {
            case CHECKERBOARD:
                return new BiomeProviderCheckerboard(this.biomes, this.size);
            case VANILLA_LAYERED:
                return new BiomeProvider(world.getWorldInfo());
            case FIXED:
                return new BiomeProviderSingle(this.biomes[0]);
            default:
                return new BiomeProviderSingle(Biomes.OCEAN);
        }
    }

    public IChunkGenerator createChunkGenerator(World world) {
        switch (this.generator) {
            case CAVES:
                return new ChunkGeneratorCaves(world, this);
            case FLOATING_ISLANDS:
                return new ChunkGeneratorFloatingIslands(world, this);
            default:
                ChunkGeneratorOverworld surface = new ChunkGeneratorOverworld(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), "");
                ((IBuffetSurface) surface).portium$setDefaults(this.defaultBlock, this.defaultFluid);
                return surface;
        }
    }

    /** The one biome of a single-biome world, or null; Buffet worlds of the Nether or End biome get their structures. */
    Biome onlyBiome() {
        if (this.source == Source.NONE) return Biomes.OCEAN;
        if (this.source == Source.FIXED) return this.biomes[0];
        if (this.source == Source.CHECKERBOARD && this.biomes.length == 1) return this.biomes[0];
        return null;
    }

    /** Swaps the stone and water a generator laid down for the world's own block and fluid. */
    public static void replaceDefaults(ChunkPrimer primer, IBlockState block, IBlockState fluid) {
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState water = Blocks.WATER.getDefaultState();
        if (block == stone && fluid == water) return;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    IBlockState state = primer.getBlockState(x, y, z);
                    if (state == stone) primer.setBlockState(x, y, z, block);
                    else if (state == water) primer.setBlockState(x, y, z, fluid);
                }
            }
        }
    }

    private static JsonObject object(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : null;
    }

    private static Biome biome(String id) {
        Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(LegacyBiomes.toLegacy(id)));
        return biome == null ? Biomes.OCEAN : biome;
    }

    private static IBlockState block(String id) {
        return LegacyBlockStates.convert(id, Collections.emptyMap());
    }
}
