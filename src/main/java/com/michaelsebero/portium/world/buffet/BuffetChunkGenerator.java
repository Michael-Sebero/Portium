package com.michaelsebero.portium.world.buffet;

import com.michaelsebero.portium.mixin.accessor.MapGenScatteredFeatureAccessor;
import com.michaelsebero.portium.world.gen.structure.OceanStructures;
import com.michaelsebero.portium.world.gen.structure.TempleLocator;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenCavesHell;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The parts of 1.13's Buffet caves and floating islands generators that don't depend on the terrain's shape. As in
 * 1.13, everything but the shape comes from the biome: its surface (built against the generator's own sea level), its
 * caves and canyons, its structures, decoration and mobs. The terrain is laid in stone and water, surfaced, and then
 * swapped for the world's default block and fluid, so that caves, ores and surfaces treat other blocks as 1.13 does.
 */
abstract class BuffetChunkGenerator implements IChunkGenerator {

    protected final World world;
    protected final Random rand;
    private final BuffetSettings settings;
    private final boolean mapFeatures;
    private final NoiseGeneratorPerlin surfaceNoise;
    private double[] depthBuffer = new double[256];
    private Biome[] biomes;
    private final List<MapGenBase> carvers = new ArrayList<>();
    /** Structure generators by the name {@code /locate} uses. */
    private final Map<String, MapGenStructure> structures = new LinkedHashMap<>();
    @Nullable
    private final MapGenScatteredFeature scatteredFeatures;
    @Nullable
    private final StructureOceanMonument monuments;

    protected BuffetChunkGenerator(World world, BuffetSettings settings) {
        this.world = world;
        this.settings = settings;
        this.rand = new Random(world.getSeed());
        this.mapFeatures = world.getWorldInfo().isMapFeaturesEnabled();
        this.surfaceNoise = new NoiseGeneratorPerlin(this.rand, 4);
        Biome only = settings.onlyBiome();
        if (only == Biomes.HELL) {
            this.carvers.add(TerrainGen.getModdedMapGen(new MapGenCavesHell(), InitMapGenEvent.EventType.NETHER_CAVE));
            this.structures.put("Fortress", (MapGenStructure) TerrainGen.getModdedMapGen(new MapGenNetherBridge(), InitMapGenEvent.EventType.NETHER_BRIDGE));
            this.scatteredFeatures = null;
            this.monuments = null;
        } else if (only == Biomes.SKY) {
            this.scatteredFeatures = null;
            this.monuments = null;
        } else {
            this.carvers.add(TerrainGen.getModdedMapGen(new MapGenCaves(), InitMapGenEvent.EventType.CAVE));
            this.carvers.add(TerrainGen.getModdedMapGen(new MapGenRavine(), InitMapGenEvent.EventType.RAVINE));
            this.structures.put("Mineshaft", (MapGenStructure) TerrainGen.getModdedMapGen(new MapGenMineshaft(), InitMapGenEvent.EventType.MINESHAFT));
            this.structures.put("Village", (MapGenStructure) TerrainGen.getModdedMapGen(new MapGenVillage(), InitMapGenEvent.EventType.VILLAGE));
            this.structures.put("Stronghold", (MapGenStructure) TerrainGen.getModdedMapGen(new MapGenStronghold(), InitMapGenEvent.EventType.STRONGHOLD));
            this.scatteredFeatures = (MapGenScatteredFeature) TerrainGen.getModdedMapGen(new MapGenScatteredFeature(), InitMapGenEvent.EventType.SCATTERED_FEATURE);
            this.monuments = (StructureOceanMonument) TerrainGen.getModdedMapGen(new StructureOceanMonument(), InitMapGenEvent.EventType.OCEAN_MONUMENT);
            this.structures.put("Temple", this.scatteredFeatures);
            this.structures.put("Monument", this.monuments);
        }
    }

    /** Lays the terrain's shape in stone, with water as its fluid. */
    protected abstract void shapeTerrain(int chunkX, int chunkZ, ChunkPrimer primer);

    /** The sea level the biome surface is built against: 1.13's caves generator uses 32 and its islands 0. */
    protected abstract int surfaceSeaLevel();

    /** Anything after the surface, such as the caves generator's bedrock. */
    protected void finishTerrain(ChunkPrimer primer) {}

    @Override
    public Chunk generateChunk(int x, int z) {
        this.rand.setSeed(x * 341873128712L + z * 132897987541L);
        ChunkPrimer primer = new ChunkPrimer();
        this.shapeTerrain(x, z, primer);
        this.biomes = this.world.getBiomeProvider().getBiomes(this.biomes, x * 16, z * 16, 16, 16);
        this.buildSurface(x, z, primer);
        this.finishTerrain(primer);
        BuffetSettings.replaceDefaults(primer, this.settings.defaultBlock, this.settings.defaultFluid);
        for (MapGenBase carver : this.carvers) carver.generate(this.world, x, z, primer);
        if (this.mapFeatures) {
            for (MapGenStructure structure : this.structures.values()) structure.generate(this.world, x, z, primer);
        }
        Chunk chunk = new Chunk(this.world, primer, x, z);
        byte[] ids = chunk.getBiomeArray();
        for (int i = 0; i < ids.length; i++) ids[i] = (byte) Biome.getIdForBiome(this.biomes[i]);
        chunk.generateSkylightMap();
        return chunk;
    }

    private void buildSurface(int x, int z, ChunkPrimer primer) {
        if (!ForgeEventFactory.onReplaceBiomeBlocks(this, x, z, primer, this.world)) return;
        this.depthBuffer = this.surfaceNoise.getRegion(this.depthBuffer, x * 16, z * 16, 16, 16, 0.0625D, 0.0625D, 1.0D);
        // 1.12's biome surfaces read the world's sea level; give them this generator's while they run.
        int seaLevel = this.world.getSeaLevel();
        this.world.setSeaLevel(this.surfaceSeaLevel());
        try {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    Biome biome = this.biomes[dz + dx * 16];
                    biome.genTerrainBlocks(this.world, this.rand, primer, x * 16 + dx, z * 16 + dz, this.depthBuffer[dz + dx * 16]);
                }
            }
        } finally {
            this.world.setSeaLevel(seaLevel);
        }
    }

    @Override
    public void populate(int x, int z) {
        BlockFalling.fallInstantly = true;
        int blockX = x * 16;
        int blockZ = z * 16;
        BlockPos corner = new BlockPos(blockX, 0, blockZ);
        Biome biome = this.world.getBiome(corner.add(16, 0, 16));
        this.rand.setSeed(this.world.getSeed());
        long xFactor = this.rand.nextLong() / 2L * 2L + 1L;
        long zFactor = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed(x * xFactor + z * zFactor ^ this.world.getSeed());
        ChunkPos chunk = new ChunkPos(x, z);
        boolean village = false;
        ForgeEventFactory.onChunkPopulate(true, this, this.world, this.rand, x, z, false);
        if (this.mapFeatures) {
            for (Map.Entry<String, MapGenStructure> structure : this.structures.entrySet()) {
                boolean placed = structure.getValue().generateStructure(this.world, this.rand, chunk);
                if ("Village".equals(structure.getKey())) village = placed;
            }
        }
        if (biome != Biomes.DESERT && biome != Biomes.DESERT_HILLS && !village && this.rand.nextInt(4) == 0
                && TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.LAKE)) {
            new WorldGenLakes(Blocks.WATER).generate(this.world, this.rand, corner.add(this.rand.nextInt(16) + 8, this.rand.nextInt(256), this.rand.nextInt(16) + 8));
        }
        if (!village && this.rand.nextInt(8) == 0 && TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.LAVA)) {
            int lakeX = this.rand.nextInt(16) + 8;
            int lakeY = this.rand.nextInt(this.rand.nextInt(248) + 8);
            int lakeZ = this.rand.nextInt(16) + 8;
            if (lakeY < this.world.getSeaLevel() || this.rand.nextInt(10) == 0) {
                new WorldGenLakes(Blocks.LAVA).generate(this.world, this.rand, corner.add(lakeX, lakeY, lakeZ));
            }
        }
        if (TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.DUNGEON)) {
            for (int i = 0; i < 8; i++) {
                new WorldGenDungeons().generate(this.world, this.rand, corner.add(this.rand.nextInt(16) + 8, this.rand.nextInt(256), this.rand.nextInt(16) + 8));
            }
        }
        biome.decorate(this.world, this.rand, corner);
        if (TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.ANIMALS)) {
            WorldEntitySpawner.performWorldGenSpawning(this.world, biome, blockX + 8, blockZ + 8, 16, 16, this.rand);
        }
        corner = corner.add(8, 0, 8);
        if (TerrainGen.populate(this, this.world, this.rand, x, z, village, PopulateChunkEvent.Populate.EventType.ICE)) {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    BlockPos top = this.world.getPrecipitationHeight(corner.add(dx, 0, dz));
                    BlockPos below = top.down();
                    if (this.world.canBlockFreezeWater(below)) this.world.setBlockState(below, Blocks.ICE.getDefaultState(), 2);
                    if (this.world.canSnowAt(top, true)) this.world.setBlockState(top, Blocks.SNOW_LAYER.getDefaultState(), 2);
                }
            }
        }
        ForgeEventFactory.onChunkPopulate(false, this, this.world, this.rand, x, z, village);
        BlockFalling.fallInstantly = false;
    }

    @Override
    public boolean generateStructures(Chunk chunk, int x, int z) {
        return this.mapFeatures && this.monuments != null && chunk.getInhabitedTime() < 3600L
                && this.monuments.generateStructure(this.world, this.rand, new ChunkPos(x, z));
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        Biome biome = this.world.getBiome(pos);
        if (this.mapFeatures) {
            MapGenStructure fortress = this.structures.get("Fortress");
            if (type == EnumCreatureType.MONSTER && fortress instanceof MapGenNetherBridge
                    && (fortress.isInsideStructure(pos) || fortress.isPositionInStructure(this.world, pos)
                    && this.world.getBlockState(pos.down()).getBlock() == Blocks.NETHER_BRICK)) {
                return ((MapGenNetherBridge) fortress).getSpawnList();
            }
            if (type == EnumCreatureType.MONSTER && this.scatteredFeatures != null && this.scatteredFeatures.isSwampHut(pos)) {
                return this.scatteredFeatures.getMonsters();
            }
            if (type == EnumCreatureType.MONSTER && this.monuments != null && this.monuments.isPositionInStructure(this.world, pos)) {
                return this.monuments.getMonsters();
            }
        }
        return biome.getSpawnableList(type);
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, String name, BlockPos pos, boolean findUnexplored) {
        if (OceanStructures.isOurs(name)) return OceanStructures.findNearest(world, name, pos);
        if (TempleLocator.isTempleName(name)) {
            return this.mapFeatures && this.scatteredFeatures != null
                    ? TempleLocator.findNearest(world, name, pos, ((MapGenScatteredFeatureAccessor) this.scatteredFeatures).portium$getSpacing()) : null;
        }
        MapGenStructure structure = this.mapFeatures ? this.structures.get(name) : null;
        return structure == null ? null : structure.getNearestStructurePos(world, pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(World world, String name, BlockPos pos) {
        MapGenStructure structure = this.mapFeatures ? this.structures.get(name) : null;
        return structure != null && structure.isInsideStructure(pos);
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
        if (!this.mapFeatures) return;
        for (MapGenStructure structure : this.structures.values()) structure.generate(this.world, x, z, null);
    }
}
