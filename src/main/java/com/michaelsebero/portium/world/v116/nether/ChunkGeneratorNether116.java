package com.michaelsebero.portium.world.v116.nether;

import com.michaelsebero.portium.world.v116.noise.SeedRandom;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 1.16's Nether for worlds of 1.16.5 generation: its terrain and lava sea, biome surfaces, bedrock and caves, then
 * each biome's features as the chunk populates. Fortresses are still 1.12's for now. Other mods hook in as they do
 * with 1.12's Nether: the populate and decorate events fire, and their own world generators run after.
 */
public class ChunkGeneratorNether116 implements IChunkGenerator {

    private final World world;
    private final boolean generateStructures;
    private final long seed;
    private final BiomeProviderNether116 biomes;
    private final NetherTerrain terrain;
    private final NetherSurface surface;
    private final NetherCaves caves;
    private final NetherDecoration decoration;
    private final Random rand = new Random();
    private MapGenNetherBridge fortress = new MapGenNetherBridge();

    public ChunkGeneratorNether116(World world, boolean generateStructures, long seed, BiomeProviderNether116 biomes) {
        this.world = world;
        this.generateStructures = generateStructures;
        this.seed = seed;
        this.biomes = biomes;
        this.terrain = new NetherTerrain(seed);
        this.surface = new NetherSurface(seed);
        this.caves = new NetherCaves(seed);
        this.decoration = new NetherDecoration(generateStructures);
        this.fortress = (MapGenNetherBridge) TerrainGen.getModdedMapGen(this.fortress, InitMapGenEvent.EventType.NETHER_BRIDGE);
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        this.rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L);
        ChunkPrimer primer = new ChunkPrimer();
        this.terrain.fill(x, z, primer);
        Biome[] columns = this.biomes.getBiomes(null, x * 16, z * 16, 16, 16);
        // One random for every column's surface and then the bedrock, as 1.16 shares it (x outer, z inner).
        SeedRandom random = new SeedRandom();
        random.setBaseChunkSeed(x, z);
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = x * 16 + localX;
                int blockZ = z * 16 + localZ;
                this.surface.build(columns[localX + localZ * 16], random, primer, blockX, blockZ, this.terrain.surfaceNoise(blockX, blockZ, localX));
            }
        }
        NetherSurface.bedrock(primer, random);
        this.caves.carve(x, z, primer);
        if (this.generateStructures) this.fortress.generate(this.world, x, z, primer);
        Chunk chunk = new Chunk(this.world, primer, x, z);
        byte[] ids = chunk.getBiomeArray();
        for (int i = 0; i < ids.length; i++) ids[i] = (byte) Biome.getIdForBiome(columns[i]);
        chunk.generateSkylightMap();
        chunk.resetRelightChecks();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
        BlockFalling.fallInstantly = true;
        // A random of the chunk's own for the fortress and other mods, as 1.12's Overworld seeds it.
        this.rand.setSeed(this.world.getSeed());
        long a = this.rand.nextLong() / 2L * 2L + 1L;
        long b = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed((long) x * a + (long) z * b ^ this.world.getSeed());
        ForgeEventFactory.onChunkPopulate(true, this, this.world, this.rand, x, z, false);
        BlockPos corner = new BlockPos(x * 16, 0, z * 16);
        ChunkPos chunkPos = new ChunkPos(x, z);
        // The area 1.12 populates is centred on the chunk corner 8 blocks in from this one's; 1.16 decorates by the cell there.
        Biome biome = this.biomes.cellBiome((x << 2) + 4, (z << 2) + 4);
        Runnable structures = () -> this.fortress.generateStructure(this.world, this.rand, chunkPos);
        this.decoration.decorate(this.world, this.seed, x, z, corner.add(8, 0, 8), biome, structures);
        ForgeEventFactory.onChunkPopulate(false, this, this.world, this.rand, x, z, false);
        // What 1.12's Nether biome decoration fires, for mods that add to it.
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(this.world, this.rand, chunkPos));
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(this.world, this.rand, corner));
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(this.world, this.rand, corner));
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(this.world, this.rand, chunkPos));
        BlockFalling.fallInstantly = false;
    }

    @Override
    public boolean generateStructures(Chunk chunk, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        if (type == EnumCreatureType.MONSTER) {
            if (this.fortress.isInsideStructure(pos)) return this.fortress.getSpawnList();
            if (this.fortress.isPositionInStructure(this.world, pos) && this.world.getBlockState(pos.down()).getBlock() == Blocks.NETHER_BRICK) {
                return this.fortress.getSpawnList();
            }
        }
        return this.world.getBiome(pos).getSpawnableList(type);
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, String structureName, BlockPos position, boolean findUnexplored) {
        return "Fortress".equals(structureName) && this.fortress != null ? this.fortress.getNearestStructurePos(world, position, findUnexplored) : null;
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
        this.fortress.generate(this.world, x, z, null);
    }

    @Override
    public boolean isInsideStructure(World world, String structureName, BlockPos pos) {
        return "Fortress".equals(structureName) && this.fortress != null && this.fortress.isInsideStructure(pos);
    }
}
